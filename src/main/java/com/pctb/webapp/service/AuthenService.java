package com.pctb.webapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.pctb.webapp.dto.request.*;
import com.pctb.webapp.dto.response.LoginResponse;
import com.pctb.webapp.dto.response.LogoutResponse;
import com.pctb.webapp.dto.response.RefreshTokenTestResponse;
import com.pctb.webapp.dto.response.RegisterResponse;
import com.pctb.webapp.dto.response.TokenInfoResponse;
import com.pctb.webapp.entity.Role;
import com.pctb.webapp.entity.RoleEnum;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.RoleRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenService {
    static String LOGIN_ATTEMPT_PREFIX = "auth:login-attempt:";
    static String REFRESH_TOKEN_PREFIX = "auth:refresh:";
    static String BLACKLIST_PREFIX = "auth:blacklist:";
    static String BEARER_PREFIX = "Bearer ";
    static String OTP_FORGOT_PREFIX = "OTP_FORGOT:";
    static String RESET_TOKEN_PREFIX = "RESET_TOKEN:";

    UserRepo userRepo;

    RoleRepo roleRepo;
    PasswordEncoder passwordEncoder;
    OtpService otpService;
    RedisService redisService;
    ObjectMapper objectMapper;
    StringRedisTemplate redisTemplate;
    MailService mailService;

    @Value("${google.client-id}")
    @NonFinal
    String googleClientId;

    @Value("${jwt.signerKey}")
    @NonFinal
    String jwtSignerKey;

    @Value("${app.otp.resend-cooldown-seconds}")
    @NonFinal
    long resendCooldownSeconds;

    @Value("${app.auth.access-token-valid-seconds:60}")
    @NonFinal
    long accessTokenValidSeconds;

    @Value("${app.auth.refresh-token-valid-seconds:604800}")
    @NonFinal
    long refreshTokenValidSeconds;

    @Value("${app.auth.login-attempt-limit:5}")
    @NonFinal
    long loginAttemptLimit;

    @Value("${app.auth.login-attempt-window-seconds:300}")
    @NonFinal
    long loginAttemptWindowSeconds;

    @Value("${app.register.pending-ttl-seconds:300}")
    @NonFinal
    long pendingRegisterTtlSeconds;

// Hàm đăng kí
    public RegisterResponse register(RegisterRequest request) {
        // Kiểm tra xem email đã tồn tại chưa
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        // Kiểm tra xem username đã tồn tại chưa
        if (userRepo.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        // Kiểm tra xem password == confirm password không
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.CONFIRM_PASSWORD_NOT_MATCH);
        }
        // Mã hóa password
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // Dùng pending để lưu tạm thông tin người dùng đã nhập trước khi xác thực
        PendingRegisterRequest pending = PendingRegisterRequest.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .fullname(request.getFullname())
                .encodedPassword(encodedPassword)
                .build();
        String pendingJson;
        // Chuyển qua JSON vì redis chỉ tương tác dạng Stin
        try {
             pendingJson = objectMapper.writeValueAsString(pending);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        // Lưu dưới redis
        redisService.setWithTtl(pendingRegisterKey(request.getEmail()),pendingJson ,
                pendingRegisterTtlSeconds);

        otpService.sendOtpAfterRegister(request.getEmail());

        return RegisterResponse.builder()
                .fullname(request.getFullname())
                .email(request.getEmail())
                .username(request.getUsername())
                .build();
    }

    public LoginResponse login(LoginRequest request) {
        String identifier = request.getIdentifier().trim();

        User user = userRepo.findByEmailOrUsername(identifier, identifier)
                .orElseThrow(() -> {
                    recordFailedLoginAttempt(identifier);
                    return new AppException(ErrorCode.USERNAME_NOT_EXISTED);
                });

        if (!user.isVerified()) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordFailedLoginAttempt(identifier);
            throw new AppException(ErrorCode.USERNAME_OR_PASSWORD_INCORRECT);
        }

        clearFailedLoginAttempt(identifier);

        String accessToken = generateToken(user, accessTokenValidSeconds, "access");
        String refreshToken = generateToken(user, refreshTokenValidSeconds, "refresh");

        redisService.setWithTtl(refreshTokenKey(user.getId()), refreshToken, refreshTokenValidSeconds);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    public LoginResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_REQUIRED);
        }

        JWTClaimsSet refreshClaims = parseAndValidateToken(
                refreshToken,
                "refresh",
                ErrorCode.REFRESH_TOKEN_INVALID
        );

        String userId = refreshClaims.getSubject();
        if (userId == null) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String key = refreshTokenKey(userId);
        String savedRefreshToken = redisService.get(key);
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USERNAME_NOT_EXISTED));
        if (!user.isVerified()) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }

        String newAccessToken = generateToken(user, accessTokenValidSeconds, "access");

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    public RefreshTokenTestResponse testNewAccessToken(RefreshTokenRequest request) {
        LoginResponse refreshedToken = refreshToken(request);
        testToken(refreshedToken.getAccessToken());

        return RefreshTokenTestResponse.builder()
                .accessToken(refreshedToken.getAccessToken())
                .refreshToken(refreshedToken.getRefreshToken())
                .build();
    }

    public LogoutResponse logout(LogoutRequest request, String accessToken) {
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_REQUIRED);
        }

        if (accessToken == null || accessToken.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        JWTClaimsSet accessClaims = parseAndValidateToken(
                accessToken,
                "access",
                ErrorCode.UNAUTHENTICATED
        );
        JWTClaimsSet refreshClaims = parseAndValidateToken(
                refreshToken,
                "refresh",
                ErrorCode.REFRESH_TOKEN_INVALID
        );

        String accessUserId = accessClaims.getSubject();
        String refreshUserId = refreshClaims.getSubject();

        if (accessUserId == null || refreshUserId == null || !accessUserId.equals(refreshUserId)) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String key = refreshTokenKey(refreshUserId);
        String savedRefreshToken = redisService.get(key);

        if (savedRefreshToken == null) {
            throw new AppException(ErrorCode.ACCOUNT_ALREADY_LOGGED_OUT);
        }

        if (!savedRefreshToken.equals(refreshToken)) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        redisService.delete(key);

        return LogoutResponse.builder()
                .loggedOut(true)
                .build();
    }

    public TokenInfoResponse testToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        JWTClaimsSet claims = parseAndValidateSupportedToken(
                accessToken,
                ErrorCode.UNAUTHENTICATED
        );

        User user = userRepo.findById(claims.getSubject())
                .orElseThrow(() -> new AppException(ErrorCode.USERNAME_NOT_EXISTED));

        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .toList();

        return TokenInfoResponse.builder()
                .valid(true)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullname(user.getFullname())
                .verified(user.isVerified())
                .roles(roles)
                .tokenType(toClaimString(claims.getClaim("tokenType")))
                .issuer(claims.getIssuer())
                .jwtId(claims.getJWTID())
                .issuedAt(toIsoString(claims.getIssueTime()))
                .expiresAt(toIsoString(claims.getExpirationTime()))
                .build();
    }

    private JWTClaimsSet parseAndValidateSupportedToken(String token, ErrorCode invalidTokenError) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            boolean verified = signedJWT.verify(new MACVerifier(jwtSignerKey.getBytes(StandardCharsets.UTF_8)));
            if (!verified) {
                throw new AppException(invalidTokenError);
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                throw new AppException(ErrorCode.TOKEN_EXPIRED);
            }

            String tokenType = claims.getStringClaim("tokenType");
            if (!"access".equals(tokenType) && !"refresh".equals(tokenType)) {
                throw new AppException(invalidTokenError);
            }

            if ("refresh".equals(tokenType)) {
                String savedRefreshToken = redisService.get(refreshTokenKey(claims.getSubject()));
                if (savedRefreshToken == null || !savedRefreshToken.equals(token)) {
                    throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
                }
            }

            return claims;
        } catch (ParseException | JOSEException e) {
            throw new AppException(invalidTokenError);
        }
    }

    private JWTClaimsSet parseAndValidateToken(String token, String expectedType, ErrorCode invalidTokenError) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            boolean verified = signedJWT.verify(new MACVerifier(jwtSignerKey.getBytes(StandardCharsets.UTF_8)));
            if (!verified) {
                throw new AppException(invalidTokenError);
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime == null || expirationTime.before(new Date())) {
                throw new AppException(ErrorCode.TOKEN_EXPIRED);
            }

            String tokenType = claims.getStringClaim("tokenType");
            if (!expectedType.equals(tokenType)) {
                throw new AppException(invalidTokenError);
            }

            return claims;
        } catch (ParseException | JOSEException e) {
            throw new AppException(invalidTokenError);
        }
    }

    private String toIsoString(Date date) {
        if (date == null) {
            return null;
        }
        return date.toInstant().toString();
    }

    private String toClaimString(Object claim) {
        if (claim == null) {
            return null;
        }
        return claim.toString();
    }

    private String generateToken(User user, long validSeconds, String tokenType) {
        try {
            Instant now = Instant.now();
            String scope = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.joining(" "));

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getId())
                    .issuer("ash-project-be")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(validSeconds)))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("scope", scope)
                    .claim("tokenType", tokenType)
                    .claim("username", user.getUsername())
                    .claim("email", user.getEmail())
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS512),
                    claimsSet
            );
            signedJWT.sign(new MACSigner(jwtSignerKey.getBytes(StandardCharsets.UTF_8)));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Cannot create token", e);
        }
    }

    private void recordFailedLoginAttempt(String identifier) {
        String key = loginAttemptKey(identifier);
        Long attempts = redisService.increment(key);

        if (attempts != null && attempts == 1) {
            redisService.expire(key, loginAttemptWindowSeconds);
        }

        if (attempts != null && attempts >= loginAttemptLimit) {
            throw new AppException(ErrorCode.LOGIN_ATTEMPTS_EXCEEDED);
        }
    }

    private void clearFailedLoginAttempt(String identifier) {
        redisService.delete(loginAttemptKey(identifier));
    }

    private String loginAttemptKey(String identifier) {
        return "auth:login:attempts:" + identifier.toLowerCase(Locale.ROOT);
    }

    private String refreshTokenKey(String userId) {
        return "auth:refresh:" + userId;
    }

    private String pendingRegisterKey(String email) {
        return "register:pending:" + email;
    }

    public void sendOtpForgotPassword(ForgotPasswordRequest request) {

        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new AppException(ErrorCode.EMAIL_NOT_EXISTED));

        String otp = otpService.generateOtp();
        redisTemplate.opsForValue().set(
                OTP_FORGOT_PREFIX + user.getEmail(),
                otp,
                5,
                TimeUnit.MINUTES
        );

        mailService.sendOtp(user.getEmail(), otp);
    }

    public String verifyOtpForgotPassword(
            VerifyOtpRequest request) {

        String savedOtp = redisTemplate.opsForValue()
                .get(OTP_FORGOT_PREFIX + request.getEmail());

        if (savedOtp == null) {
            throw new AppException(
                    ErrorCode.FORGOT_PASSWORD_OTP_EXPIRED);
        }

        if (!savedOtp.equals(request.getOtp())) {
            throw new AppException(
                    ErrorCode.FORGOT_PASSWORD_OTP_INVALID);
        }

        redisTemplate.delete(
                OTP_FORGOT_PREFIX + request.getEmail());

        String resetToken = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(
                RESET_TOKEN_PREFIX + resetToken,
                request.getEmail(),
                5,
                TimeUnit.MINUTES
        );

        return resetToken;
    }

    public void resetPassword(
            ResetPasswordRequest request) {

        if (!request.getNewPassword()
                .equals(request.getConfirmPassword())) {

            throw new AppException(
                    ErrorCode.RESET_PASSWORD_MISMATCH);
        }

        String email = redisTemplate.opsForValue()
                .get(RESET_TOKEN_PREFIX
                        + request.getResetToken());

        if (email == null) {
            throw new AppException(
                    ErrorCode.RESET_TOKEN_INVALID);
        }

        User user = userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new AppException(
                                ErrorCode.EMAIL_NOT_EXISTED));

        user.setPassword(
                passwordEncoder.encode(
                        request.getNewPassword()));

        userRepo.save(user);

        redisTemplate.delete(
                RESET_TOKEN_PREFIX
                        + request.getResetToken());
    }

    public void resendForgotPasswordOtp(ForgotPasswordRequest request) {
        sendOtpForgotPassword(request);
    }

    private GoogleIdToken.Payload verifyGoogleToken(String token) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(token);

            if (idToken == null) {
                throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            if (!Boolean.TRUE.equals(payload.getEmailVerified())) {
                throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID);
            }

            return payload;
        } catch (Exception e) {
            throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID);
        }
    }

    private String generateGoogleUsername(String email) {
        String baseUsername = email.substring(0, email.indexOf("@"))
                .replaceAll("[^a-zA-Z0-9]", "");

        if (baseUsername.length() < 3) {
            baseUsername = "user";
        }

        String username = baseUsername;
        int counter = 1;

        while (userRepo.existsByUsername(username)) {
            username = baseUsername + counter;
            counter++;
        }

        return username;
    }

    private User createGoogleUser(String email, String fullname) {
        Role userRole = roleRepo.findById(RoleEnum.USER.name())
                .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND));

        User user = User.builder()
                .email(email)
                .username(generateGoogleUsername(email))
                .fullname(fullname != null && !fullname.isBlank() ? fullname : email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .verified(true)
                .roles(Set.of(userRole))
                .build();

        return userRepo.save(user);
    }

    public LoginResponse googleLogin(GoogleLoginRequest request) {
        GoogleIdToken.Payload payload = verifyGoogleToken(request.getToken());

        String email = payload.getEmail();
        String fullname = (String) payload.get("name");

        User user = userRepo.findByEmail(email)
                .orElseGet(() -> createGoogleUser(email, fullname));

        if (!user.isVerified()) {
            user.setVerified(true);
            userRepo.save(user);
        }

        String accessToken = generateToken(user, accessTokenValidSeconds, "access");
        String refreshToken = generateToken(user, refreshTokenValidSeconds, "refresh");

        redisService.setWithTtl(refreshTokenKey(user.getId()), refreshToken, refreshTokenValidSeconds);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }
}
