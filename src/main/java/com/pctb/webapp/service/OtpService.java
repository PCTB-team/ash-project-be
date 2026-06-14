package com.pctb.webapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pctb.webapp.dto.request.PendingRegisterRequest;
import com.pctb.webapp.dto.response.OtpResponse;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OtpService {
    final RedisService redisService;
    final UserRepo userRepo;
    final MailService mailService;
    final ObjectMapper objectMapper;
    final RoleRepo roleRepo;

    SecureRandom secureRandom = new SecureRandom();

    @Value("${app.otp.ttl-seconds}")
    long otpTtlSeconds;

    @Value("${app.otp.resend-cooldown-seconds}")
    long resendCooldownSeconds;

    @Value("${app.otp.max-send-per-day}")
    long maxSendPerDay;

    @Value("${app.otp.limit-ttl-seconds}")
    long limitTtlSeconds;

    // Tạo OTP mới sau bước đăng ký, lưu OTP vào Redis có TTL, đánh dấu cooldown và gửi email cho user.
    public void sendOtpAfterRegister(String email) {
        validateOtpSendAvailable(email);

        String otp = generateOtp();
        redisService.setWithTtl(otpKey(email), otp, otpTtlSeconds);
        markOtpSent(email);

        mailService.sendOtp(email, otp);

        System.out.println("OTP for " + email + ": " + otp);
    }

    // Gửi lại OTP cho email đang có phiên đăng ký pending và chưa được verify thành tài khoản thật.
    public OtpResponse resendOtp(String email) {
        if (userRepo.existsByEmail(email)) {
            throw new AppException(ErrorCode.ACCOUNT_ALREADY_VERIFIED);
        }

        String pendingJson = redisService.get(registerKey(email));
        if (pendingJson == null) {
            throw new AppException(ErrorCode.REGISTER_SESSION_EXPIRED);
        }

        sendOtpAfterRegister(email);

        return OtpResponse.builder()
                .email(email)
                .build();
    }

    // Xác thực OTP đăng ký, đọc thông tin pending từ Redis, tạo user thật trong DB và xóa các key tạm.
    public OtpResponse verifyOtp(String email, String otp) {
        String pendingJson = redisService.get(registerKey(email));
        if (pendingJson == null) {
            throw new AppException(ErrorCode.REGISTER_SESSION_EXPIRED);
        }

        String savedOtp = redisService.get(otpKey(email));
        if (savedOtp == null) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        if (!savedOtp.equals(otp)) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        PendingRegisterRequest pending;
        try {
            pending = objectMapper.readValue(pendingJson, PendingRegisterRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        if (userRepo.existsByEmail(pending.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (userRepo.existsByUsername(pending.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }

        Role userRole = roleRepo.findById(RoleEnum.USER.name())
                .orElseGet(() -> roleRepo.save(
                        new Role(RoleEnum.USER.name(), "User role")
                ));

        User user = User.builder()
                .username(pending.getUsername())
                .email(pending.getEmail())
                .fullname(pending.getFullname())
                .password(pending.getEncodedPassword())
                .verified(true)
                .roles(Set.of(userRole))
                .build();

        userRepo.save(user);

        redisService.delete(otpKey(email));
        redisService.delete(cooldownKey(email));
        redisService.delete(registerKey(email));

        return OtpResponse.builder()
                .email(user.getEmail())
                .verified(true)
                .build();
    }

    // Kiểm tra email có được phép gửi OTP tại thời điểm hiện tại hay không, gồm giới hạn ngày và cooldown.
    public void validateOtpSendAvailable(String email) {
        checkLimit(email);
        checkCooldown(email);
    }

    // Đánh dấu một lần gửi OTP vừa xảy ra bằng cách tạo cooldown key và tăng bộ đếm gửi trong ngày.
    public void markOtpSent(String email) {
        redisService.setWithTtl(cooldownKey(email), "1", resendCooldownSeconds);
        increaseLimit(email);
    }

    // Kiểm tra số lần gửi OTP trong ngày của email có vượt quá giới hạn cấu hình hay chưa.
    private void checkLimit(String email) {
        String current = redisService.get(limitKey(email));

        if (current != null && Long.parseLong(current) >= maxSendPerDay) {
            throw new AppException(ErrorCode.OTP_SEND_LIMIT_EXCEEDED);
        }
    }

    // Kiểm tra email có đang trong thời gian cooldown chưa được gửi lại OTP hay không.
    private void checkCooldown(String email) {
        if (redisService.get(cooldownKey(email)) != null) {
            throw new AppException(ErrorCode.OTP_RESEND_TOO_SOON);
        }
    }

    // Tăng bộ đếm số lần gửi OTP và gán TTL cho key ở lần tăng đầu tiên.
    private void increaseLimit(String email) {
        String key = limitKey(email);
        Long count = redisService.increment(key);

        if (count != null && count == 1) {
            redisService.expire(key, limitTtlSeconds);
        }
    }

    // Tạo Redis key lưu thông tin đăng ký pending theo email.
    private String registerKey(String email) {
        return "register:pending:" + email;
    }

    // Sinh mã OTP 6 chữ số bằng SecureRandom để dùng cho xác thực email.
    public String generateOtp() {
        int otp = secureRandom.nextInt(900000) + 100000;
        return String.valueOf(otp);
    }

    // Tạo Redis key đếm số lần gửi OTP trong ngày theo email.
    private String limitKey(String email) {
        return "otp:limit:" + email;
    }

    // Tạo Redis key lưu OTP hiện tại của email.
    private String otpKey(String email) {
        return "otp:" + email;
    }

    // Tạo Redis key cooldown để chặn gửi lại OTP quá nhanh.
    private String cooldownKey(String email) {
        return "otp:cooldown:" + email;
    }
}
