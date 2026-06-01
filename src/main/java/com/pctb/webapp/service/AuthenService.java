package com.pctb.webapp.service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.pctb.webapp.dto.request.LoginRequest;
import com.pctb.webapp.dto.request.LogoutRequest;
import com.pctb.webapp.dto.request.RegisterRequest;
import com.pctb.webapp.dto.response.LoginResponse;
import com.pctb.webapp.dto.response.LoginUserResponse;
import com.pctb.webapp.dto.response.RegisterResponse;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;


import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenService {
    static String LOGIN_ATTEMPT_PREFIX = "auth:login-attempt:";
    static String REFRESH_TOKEN_PREFIX = "auth:refresh:";
    static String BLACKLIST_PREFIX = "auth:blacklist:";
    static String BEARER_PREFIX = "Bearer ";

    UserRepo userRepo;
    RoleRepo roleRepo;
    PasswordEncoder passwordEncoder;
    StringRedisTemplate stringRedisTemplate;
    JwtDecoder jwtDecoder;

    @NonFinal
    @Value("${jwt.signerKey}")
    String jwtSignerKey;

    @NonFinal
    @Value("${jwt.accessTokenDuration:3600}")
    long accessTokenDuration;

    @NonFinal
    @Value("${jwt.refreshTokenDuration:604800}")
    long refreshTokenDuration;

    @NonFinal
    @Value("${jwt.loginAttemptLimit:5}")
    long loginAttemptLimit;

    @NonFinal
    @Value("${jwt.loginAttemptWindowSeconds:300}")
    long loginAttemptWindowSeconds;



    public RegisterResponse register(RegisterRequest request) {
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (userRepo.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.CONFIRM_PASSWORD_NOT_MATCH);
        }

        Role userRole = roleRepo.findById(RoleEnum.USER.name())
                .orElseGet(() -> roleRepo.save(new Role(RoleEnum.USER.name(), "User role")));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullname(request.getFullname())
                .roles(Set.of(userRole))
                .build();

        userRepo.save(user);

        return RegisterResponse.builder()
                .fullname(user.getFullname())
                .email(user.getEmail())
                .username(user.getUsername())
                .build();
    }

    public LoginResponse login(LoginRequest request) {
        String identifier = request.getIdentifier().trim();
        checkLoginAttemptLimit(identifier);

        User user = userRepo.findByEmail(identifier)
                .or(() -> userRepo.findByUsername(identifier))
                .orElseThrow(() -> {
                    recordFailedLoginAttempt(identifier);
                    return new AppException(ErrorCode.INVALID_CREDENTIALS);
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            recordFailedLoginAttempt(identifier);
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (!user.isVerified()) {
            throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
        }

        clearLoginAttempts(identifier);

        String accessToken = generateAccessToken(user);
        String refreshToken = UUID.randomUUID().toString();

        stringRedisTemplate.opsForValue()
                .set(refreshTokenKey(refreshToken), user.getId(), Duration.ofSeconds(refreshTokenDuration));

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenDuration)
                .user(LoginUserResponse.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .fullname(user.getFullname())
                        .build())
                .build();
    }

    public void logout(String authorizationHeader, LogoutRequest request) {
        String accessToken = extractBearerToken(authorizationHeader);

        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(blacklistKey(accessToken)))) {
            throw new AppException(ErrorCode.TOKEN_ALREADY_LOGGED_OUT);
        }

        Jwt jwt = decodeAccessToken(accessToken);

        String refreshTokenUserId = stringRedisTemplate.opsForValue().get(refreshTokenKey(request.getRefreshToken()));
        if (refreshTokenUserId == null || !refreshTokenUserId.equals(jwt.getSubject())) {
            throw new AppException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        stringRedisTemplate.delete(refreshTokenKey(request.getRefreshToken()));
        blacklistAccessToken(accessToken, jwt);
    }

    private void checkLoginAttemptLimit(String identifier) {
        String raw = stringRedisTemplate.opsForValue().get(loginAttemptKey(identifier));
        if (raw == null) {
            return;
        }

        try {
            long attempts = Long.parseLong(raw);
            if (attempts >= loginAttemptLimit) {
                throw new AppException(ErrorCode.LOGIN_ATTEMPTS_EXCEEDED);
            }
        } catch (NumberFormatException ignored) {
            clearLoginAttempts(identifier);
        }
    }

    private void recordFailedLoginAttempt(String identifier) {
        Long attempts = stringRedisTemplate.opsForValue().increment(loginAttemptKey(identifier));
        if (attempts != null && attempts == 1) {
            stringRedisTemplate.expire(loginAttemptKey(identifier), Duration.ofSeconds(loginAttemptWindowSeconds));
        }
        if (attempts != null && attempts >= loginAttemptLimit) {
            throw new AppException(ErrorCode.LOGIN_ATTEMPTS_EXCEEDED);
        }
    }

    private void clearLoginAttempts(String identifier) {
        stringRedisTemplate.delete(loginAttemptKey(identifier));
    }

    private String generateAccessToken(User user) {
        Instant now = Instant.now();
        String scope = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.joining(" "));

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(user.getId())
                .issuer("webapp")
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(accessTokenDuration)))
                .claim("scope", scope)
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .jwtID(UUID.randomUUID().toString())
                .build();

        try {
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claimsSet);
            signedJWT.sign(new MACSigner(jwtSignerKey.getBytes(StandardCharsets.UTF_8)));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Cannot create access token", e);
        }
    }

    private void blacklistAccessToken(String accessToken, Jwt jwt) {
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null) {
            throw new AppException(ErrorCode.ACCESS_TOKEN_INVALID);
        }

        long ttlSeconds = Duration.between(Instant.now(), expiresAt).getSeconds();
        if (ttlSeconds <= 0) {
            throw new AppException(ErrorCode.ACCESS_TOKEN_INVALID);
        }

        stringRedisTemplate.opsForValue().set(blacklistKey(accessToken), "1", Duration.ofSeconds(ttlSeconds));
    }

    private Jwt decodeAccessToken(String accessToken) {
        try {
            return jwtDecoder.decode(accessToken);
        } catch (JwtException ex) {
            throw new AppException(ErrorCode.ACCESS_TOKEN_INVALID);
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return token;
    }

    private String loginAttemptKey(String identifier) {
        return LOGIN_ATTEMPT_PREFIX + identifier.toLowerCase(Locale.ROOT);
    }

    private String refreshTokenKey(String refreshToken) {
        return REFRESH_TOKEN_PREFIX + refreshToken;
    }

    private String blacklistKey(String accessToken) {
        return BLACKLIST_PREFIX + accessToken;
    }

}
