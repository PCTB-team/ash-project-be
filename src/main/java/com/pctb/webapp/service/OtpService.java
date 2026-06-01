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

    // Gửi otp sau khi đăng kí
    public void sendOtpAfterRegister(String email) {
        checkLimit(email);//kiểm tra xem email đó đã gửi quá 5 lần hay chưa

        String otp = generateOtp();
        // lưu otp xuống redis
        redisService.setWithTtl(otpKey(email), otp, otpTtlSeconds);
        redisService.setWithTtl(cooldownKey(email), "1", resendCooldownSeconds);
        increaseLimit(email);

        // gửi mail
        mailService.sendOtp(email, otp);

        System.out.println("OTP for " + email + ": " + otp);
    }

    // ResendOtp
    public OtpResponse resendOtp(String email) {
        if (!redisService.hasKey(registerKey(email))) {
            throw new AppException(ErrorCode.EMAIL_NOT_FOUND);
        }

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
                .verified(false)
                .build();
    }
    // Xác thực otp
    public OtpResponse verifyOtp(String email, String otp) {

        // lấy otp đã save từ redis
        String savedOtp = redisService.get(otpKey(email));
        // Nếu không có trong redis thì tức là đã hết hạn
        if (savedOtp == null) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }
        // Nếu nhập sai otp thì ném ra là sai
        if (!savedOtp.equals(otp)) {
            throw new AppException(ErrorCode.OTP_INVALID);
        }
        // Lấy value JSON từ redis
        String pendingJson = redisService.get(registerKey(email));
        if (pendingJson == null) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }
        PendingRegisterRequest pending;
        // Chuyển JSON thanhf object
        try {
            pending = objectMapper.readValue(pendingJson, PendingRegisterRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        // Phải ktra lại vì nếu trong khoản tgian đợi verify otp thì có người khác đã đăng kí thành công cùng 1 giá trij rồi
        if (userRepo.existsByEmail(pending.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (userRepo.existsByUsername(pending.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        // Kiểm tra coi dưới DB đã có role USER chưa nếu chưa thì tạo
        Role userRole = roleRepo.findById(RoleEnum.USER.name())
                .orElseGet(() -> roleRepo.save(
                        new Role(RoleEnum.USER.name(), "User role")
                ));

        // Tạo user và lưu xuống DB
        User user = User.builder()
                .username(pending.getUsername())
                .email(pending.getEmail())
                .fullname(pending.getFullname())
                .password(pending.getEncodedPassword())
                .verified(true)
                .roles(Set.of(userRole))
                .build();

        userRepo.save(user);

        // Sau khi hoàn tất thì hủy hết toàn bộ key liên quan tới email đó
        redisService.delete(otpKey(email));
        redisService.delete(cooldownKey(email));
        redisService.delete(registerKey(email));


        return OtpResponse.builder()
                .email(user.getEmail())
                .verified(true)
                .build();
    }


    // Kiểm tra xem số lần gửi otp của email đó có việc qua 5 lần trên ngày không
    private void checkLimit(String email) {
        String current = redisService.get(limitKey(email));

        if (current != null && Long.parseLong(current) >= maxSendPerDay) {
            throw new AppException(ErrorCode.OTP_SEND_LIMIT_EXCEEDED);
        }
    }
    // tăng số lần của email mỗi lần gọi
    private void increaseLimit(String email) {
        String key = limitKey(email);
        Long count = redisService.increment(key);

        if (count != null && count == 1) {
            redisService.expire(key, limitTtlSeconds);
        }
    }
    private String registerKey(String email) {
        return "register:pending:" + email;
    }
    // Tạo otp
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
