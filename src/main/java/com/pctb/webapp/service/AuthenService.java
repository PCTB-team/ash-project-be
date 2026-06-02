package com.pctb.webapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.pctb.webapp.dto.request.LoginRequest;
import com.pctb.webapp.dto.request.PendingRegisterRequest;
import com.pctb.webapp.dto.request.RegisterRequest;
import com.pctb.webapp.dto.response.LoginResponse;
import com.pctb.webapp.dto.response.RegisterResponse;
import com.pctb.webapp.entity.Role;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
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
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

import com.pctb.webapp.dto.request.ForgotPasswordRequest;
import com.pctb.webapp.dto.request.ResetPasswordRequest;
import com.pctb.webapp.dto.request.VerifyOtpRequest;

import java.util.Random;
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

    PasswordEncoder passwordEncoder;
    OtpService otpService;
    RedisService redisService;
    ObjectMapper objectMapper;
    StringRedisTemplate redisTemplate;
    MailService mailService;
    @Value("${jwt.signerKey}")
    @NonFinal
    String jwtSignerKey;

    @Value("${app.otp.resend-cooldown-seconds}")
    @NonFinal
    long resendCooldownSeconds;

    @Value("${app.auth.access-token-valid-seconds:3600}")
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

        String otp = String.format("%06d",
                new Random().nextInt(999999));

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
}
