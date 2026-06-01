package com.pctb.webapp.service;

import com.pctb.webapp.dto.response.OtpResponse;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OtpService {
    final RedisService redisService;
    final UserRepo userRepo;
    final MailService mailService;

    SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.ttl-seconds}")
    long otpTtlSeconds;

    @Value("${app.otp.resend-cooldown-seconds}")
    long resendCooldownSeconds;

    @Value("${app.otp.max-send-per-day}")
    long maxSendPerDay;

    @Value("${app.otp.limit-ttl-seconds}")
    long limitTtlSeconds;

    public void sendOtpAfterRegister(String email) {
        checkLimit(email);

        String otp = generateOtp();

        redisService.setWithTtl(otpKey(email), otp, otpTtlSeconds);
        redisService.setWithTtl(cooldownKey(email), "1", resendCooldownSeconds);
        increaseLimit(email);

        mailService.sendOtp(email, otp);

        System.out.println("OTP for " + email + ": " + otp);
    }

    public OtpResponse resendOtp(String email) {
        User user = getUser(email);
        checkAlreadyVerified(user);

        if (redisService.hasKey(cooldownKey(email))) {
            throw new AppException(ErrorCode.OTP_RESEND_TOO_SOON);
        }

        checkLimit(email);

        String otp = generateOtp();

        redisService.setWithTtl(otpKey(email), otp, otpTtlSeconds);
        redisService.setWithTtl(cooldownKey(email), "1", resendCooldownSeconds);
        increaseLimit(email);

        mailService.sendOtp(email, otp);

        System.out.println("OTP for " + email + ": " + otp);

        return OtpResponse.builder()
                .email(email)
                .build();
    }

    public OtpResponse verifyOtp(String email, String otp) {
        User user = getUser(email);
        checkAlreadyVerified(user);

        String savedOtp = redisService.get(otpKey(email));

        if (savedOtp == null) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        if (!savedOtp.equals(otp)) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        user.setVerified(true);
        userRepo.save(user);

        redisService.delete(otpKey(email));
        redisService.delete(cooldownKey(email));

        return OtpResponse.builder()
                .email(user.getEmail())
                .verified(true)
                .build();
    }

    private User getUser(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_FOUND));
    }

    private void checkAlreadyVerified(User user) {
        if (user.isVerified()) {
            throw new AppException(ErrorCode.ACCOUNT_ALREADY_VERIFIED);
        }
    }

    private void checkLimit(String email) {
        String current = redisService.get(limitKey(email));

        if (current != null && Long.parseLong(current) >= maxSendPerDay) {
            throw new AppException(ErrorCode.OTP_SEND_LIMIT_EXCEEDED);
        }
    }

    private void increaseLimit(String email) {
        String key = limitKey(email);
        Long count = redisService.increment(key);

        if (count != null && count == 1) {
            redisService.expire(key, limitTtlSeconds);
        }
    }

    private String generateOtp() {
        int otp = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(otp);
    }

    private String limitKey(String email) {
        return "otp:limit:" + email;
    }

    private String otpKey(String email) {
        return "otp:" + email;
    }

    private String cooldownKey(String email) {
        return "otp:cooldown:" + email;
    }
}
