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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenService {

    static String OTP_FORGOT_PREFIX = "OTP_FORGOT:";
    static String RESET_TOKEN_PREFIX = "RESET_TOKEN:";

    UserRepo userRepo;

    RoleRepo roleRepo;
    PasswordEncoder passwordEncoder;
    OtpService otpService;
    RedisService redisService;
    ObjectMapper objectMapper;
    MailService mailService;

    @Value("${google.client-id}")
    @NonFinal
    String googleClientId;

    @Value("${jwt.signerKey}")
    @NonFinal
    String jwtSignerKey;

    @Value("${app.otp.ttl-seconds}")
    @NonFinal
    long otpTtlSeconds;

    @Value("${jwt.accessTokenDuration:3600}")
    @NonFinal
    long accessTokenValidSeconds;

    @Value("${jwt.refreshTokenDuration:604800}")
    @NonFinal
    long refreshTokenValidSeconds;

    @Value("${jwt.loginAttemptLimit:5}")
    @NonFinal
    long loginAttemptLimit;

    @Value("${jwt.loginAttemptWindowSeconds:300}")
    @NonFinal
    long loginAttemptWindowSeconds;

    @Value("${app.register.pending-ttl-seconds:300}")
    @NonFinal
    long pendingRegisterTtlSeconds;

// Hàm đăng kí nhận vào request
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
        // Chuyển qua JSON vì redis chỉ tương tác dạng Sting
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

    // Hàm dùng để đăng kí login
    public LoginResponse login(LoginRequest request) {
        // lấy username hoặc email của request bỏ khoẳng trắng
        String identifier = request.getIdentifier().trim();

        // Hàm kiêm tra username or email có tồn tại dưới DB chưa
        User user = userRepo.findByEmailOrUsername(identifier, identifier)
                .orElseThrow(() -> {
                    recordFailedLoginAttempt(identifier); // đánh giấu số lần login failed
                    return new AppException(ErrorCode.USERNAME_OR_PASSWORD_INCORRECT);
                });

        // Kiểm tra coi user đã được verify otp chưa đúng không
        if (!user.isVerified()) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
        // Đoạn kiểm tra mật khẩu
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordFailedLoginAttempt(identifier);
            throw new AppException(ErrorCode.USERNAME_OR_PASSWORD_INCORRECT);
        }
        // Nếu login đúng thì xóa toàn bộ nhuwgnx lần login failed trước đó
        clearFailedLoginAttempt(identifier);

        // Tạo accessToken và refreshToken
        String accessToken = generateToken(user, accessTokenValidSeconds, "access");
        String refreshToken = generateToken(user, refreshTokenValidSeconds, "refresh");

        // Lưu refresh token xuống redis với tgian là refreshTokenValidSeconds
        redisService.setWithTtl(refreshTokenKey(user.getId()), refreshToken, refreshTokenValidSeconds);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .authenticated(true)
                .build();
    }

    // Dùng để tạo access token mới cần truyền vô refreshToken
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        // Lấy refresh token
        String refreshToken = request.getRefreshToken();

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_REQUIRED);
        }
        // Dùng để kiểm tra refresh token có đúng là token được tạo từ system hay không
        JWTClaimsSet refreshClaims = parseAndValidateToken(
                refreshToken,
                "refresh",
                ErrorCode.REFRESH_TOKEN_INVALID
        );
        // User id từ claim , sub
        String userId = refreshClaims.getSubject();
        if (userId == null) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        String key = refreshTokenKey(userId);
        String savedRefreshToken = redisService.get(key);
        // Kiểm tra token được lưu có null hoặc không bằng refresh token được truyền vào hay ko => ném lỗi
        if (savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        // Lấy user từ id
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        // Ktra user đc verify chưa
        if (!user.isVerified()) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }
        // Tạo ra new accessToken từ user .
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
    // Logout truyền vào refresh token và access token
    public LogoutResponse logout(LogoutRequest request, String accessToken) {
        // Get refreshToken
        String refreshToken = request.getRefreshToken();
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_REQUIRED);
        }

        if (accessToken == null || accessToken.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        // Kiểm tra accessToken có còn hạn không
        JWTClaimsSet accessClaims = parseAndValidateToken(
                accessToken,
                "access",
                ErrorCode.UNAUTHENTICATED
        );
        // Kiểm tra refreshToken có còn hạn không
        JWTClaimsSet refreshClaims = parseAndValidateToken(
                refreshToken,
                "refresh",
                ErrorCode.REFRESH_TOKEN_INVALID
        );
        // Lấy userId của từng token để kiểm tra coi 2 token có thuộc cùng 1user  không
        String accessUserId = accessClaims.getSubject();
        String refreshUserId = refreshClaims.getSubject();

        if (accessUserId == null || refreshUserId == null || !accessUserId.equals(refreshUserId)) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        // Lấy refresh tokend dã được lưu dưới Redis
        String key = refreshTokenKey(refreshUserId);
        String savedRefreshToken = redisService.get(key);

        if (savedRefreshToken == null) {
            throw new AppException(ErrorCode.ACCOUNT_ALREADY_LOGGED_OUT);
        }

        if (!savedRefreshToken.equals(refreshToken)) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }
        // Xóa refresh token
        redisService.delete(key);

        return LogoutResponse.builder()
                .loggedOut(true)
                .build();
    }
    // Dùng để test Token
    public TokenInfoResponse testToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        JWTClaimsSet claims = parseAndValidateSupportedToken(
                accessToken,
                ErrorCode.UNAUTHENTICATED
        );

        User user = userRepo.findById(claims.getSubject())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

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
    // Dùng để test Token
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

    // Kiểm tra token nhập vào có đúng là token được tạo ra từ hệ thôống không
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
    // Dùng để tạo JWT token cần truyền user, tgian tồn tại của token, và type
    private String generateToken(User user, long validSeconds, String tokenType) {
        try {
            // Thời gian hiện tại
            Instant now = Instant.now();
            // Gắn scope cho user
            String scope = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.joining(" "));
            // Tạo jwt claim,tức là nội dung
            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getId())
                    .issuer("ash-project-be")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(validSeconds)))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("scope", scope)
                    .claim("tokenType", tokenType)
                    .claim("username", user.getUsername())
                    .claim("fullname", user.getFullname())
                    .claim("email", user.getEmail())
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS512),
                    claimsSet
            );
            // Kí xác thực jwt token
            signedJWT.sign(new MACSigner(jwtSignerKey.getBytes(StandardCharsets.UTF_8)));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Cannot create token", e);
        }
    }
    // Tăng bộ đếm login sai trong redis
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
    // Clear bộ đếm login sai trong redis
    private void clearFailedLoginAttempt(String identifier) {
        redisService.delete(loginAttemptKey(identifier));
    }

    // Key của email. username trong redis
    private String loginAttemptKey(String identifier) {
        return "auth:login:attempts:" + identifier.toLowerCase(Locale.ROOT);
    }
    // Key của refreshtoken lưu xuống redis
    private String refreshTokenKey(String userId) {
        return "auth:refresh:" + userId;
    }
    // Key của user lưu tạm xuống redis
    private String pendingRegisterKey(String email) {
        return "register:pending:" + email;
    }


    // Dùng để gửi otp dành cho quen mk
    public void sendOtpForgotPassword(ForgotPasswordRequest request) {

        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new AppException(ErrorCode.EMAIL_NOT_EXISTED));

        otpService.validateOtpSendAvailable(user.getEmail());

        String otp = otpService.generateOtp();
        redisService.setWithTtl(forgotOtpKey(user.getEmail()), otp, otpTtlSeconds);
        otpService.markOtpSent(user.getEmail());

        mailService.sendOtp(user.getEmail(), otp);
    }
    // Dfungf để xác thưực OTP
    public String verifyOtpForgotPassword(
            VerifyForgotPasswordOtpRequest request) {

        String savedOtp = redisService.get(forgotOtpKey(request.getEmail()));

        if (savedOtp == null) {
            throw new AppException(
                    ErrorCode.FORGOT_PASSWORD_OTP_EXPIRED);
        }

        if (!savedOtp.equals(request.getOtp())) {
            throw new AppException(
                    ErrorCode.FORGOT_PASSWORD_OTP_INVALID);
        }

        redisService.delete(forgotOtpKey(request.getEmail()));

        String resetToken = UUID.randomUUID().toString();

        redisService.setWithTtl(resetTokenKey(resetToken), request.getEmail(), otpTtlSeconds);

        return resetToken;
    }
    // Tạo mật khẩu mởi
    public void resetPassword(
            ResetPasswordRequest request) {

        if (!request.getNewPassword()
                .equals(request.getConfirmPassword())) {

            throw new AppException(
                    ErrorCode.RESET_PASSWORD_MISMATCH);
        }

        String email = redisService.get(resetTokenKey(request.getResetToken()));

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

        try {
            userRepo.save(user);
        } catch (RuntimeException exception) {
            throw new AppException(ErrorCode.RESET_PASSWORD_FAILED);
        }

        redisService.delete(resetTokenKey(request.getResetToken()));
    }
    // Dùng để gửi lại otp khi queên mật khẩu
    public void resendForgotPasswordOtp(ForgotPasswordRequest request) {
        sendOtpForgotPassword(request);
    }
    // Dùng để kiêm tra token của google
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
    // tạo username mới khi đăng nhập bằng google
    private String generateGoogleUsername(String email) {
        String baseUsername = email.substring(0, email.indexOf("@"))
                .replaceAll("[^a-zA-Z0-9]", "");

        if (baseUsername.length() < 3) {
            baseUsername = "user";
        }

        baseUsername = baseUsername.substring(0, Math.min(baseUsername.length(), 20));

        String username = baseUsername;
        int counter = 1;

        while (userRepo.existsByUsername(username)) {
            String suffix = String.valueOf(counter);
            int prefixLength = Math.min(baseUsername.length(), Math.max(0, 20 - suffix.length()));
            username = baseUsername.substring(0, prefixLength) + suffix;
            if (username.length() > 20) {
                username = username.substring(username.length() - 20);
            }
            counter++;
        }

        return username;
    }

    private String forgotOtpKey(String email) {
        return OTP_FORGOT_PREFIX + email;
    }

    private String resetTokenKey(String resetToken) {
        return RESET_TOKEN_PREFIX + resetToken;
    }

    // Tạo user mới khi đăng nhập bằng google
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
    // Login bằng tài khoản google
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
