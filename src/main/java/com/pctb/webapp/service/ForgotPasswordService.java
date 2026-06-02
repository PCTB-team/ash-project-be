package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.*;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ForgotPasswordService {

    UserRepo userRepo;
    StringRedisTemplate redisTemplate;
    MailService mailService;
    PasswordEncoder passwordEncoder;

    String OTP_FORGOT_PREFIX = "OTP_FORGOT:";
    String RESET_TOKEN_PREFIX = "RESET_TOKEN:";

    /**
     * STEP 1: Validate User's Email existence and trigger OTP mail dispatch via MailService
     */
    public void sendOtpForgotPassword(ForgotPasswordRequest request) {
        // Enforce account inspection to verify email exists in the database
        User user = userRepo.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXISTED));

        // Generate 6-digit random verification OTP string
        String otp = String.format("%06d", new Random().nextInt(999999));

        // Persist token onto Redis cache cluster with a 5-minute Lifespan
        redisTemplate.opsForValue().set(OTP_FORGOT_PREFIX + user.getEmail(), otp, 5, TimeUnit.MINUTES);

        // Re-use your custom MailService capability components
        mailService.sendOtp(user.getEmail(), otp);
    }

    /**
     * STEP 2: Verify input OTP value from Redis cache and return an ephemeral secure Reset-Token
     */
    public String verifyOtpForgotPassword(VerifyOtpRequest request) {
        String savedOtp = redisTemplate.opsForValue().get(OTP_FORGOT_PREFIX + request.getEmail());

        if (savedOtp == null) {
            throw new AppException(ErrorCode.FORGOT_PASSWORD_OTP_EXPIRED);
        }
        if (!savedOtp.equals(request.getOtp())) {
            throw new AppException(ErrorCode.FORGOT_PASSWORD_OTP_INVALID);
        }

        // Wipe OTP entry immediately upon successful evaluation to mitigate multi-use exploitation
        redisTemplate.delete(OTP_FORGOT_PREFIX + request.getEmail());

        // Construct unique secure transaction handle token passport (Reset-Token)
        String resetToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(RESET_TOKEN_PREFIX + resetToken, request.getEmail(), 5, TimeUnit.MINUTES);

        return resetToken;
    }

    /**
     * STEP 3: Complete password mutation transaction via Reset-Token lookup validation
     */
    public void resetPassword(ResetPasswordRequest request) {
        // 1. Kiểm tra xác nhận mật khẩu khớp nhau
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.RESET_PASSWORD_MISMATCH);
        }

        // 2. Query Redis để lấy email từ Reset Token
        String email = redisTemplate.opsForValue().get(RESET_TOKEN_PREFIX + request.getResetToken());

        // Sửa: Dùng RESET_TOKEN_INVALID thay vì RESET_PASSWORD_FAILED ở đây
        if (email == null) {
            throw new AppException(ErrorCode.RESET_TOKEN_INVALID);
        }

        // 3. Tìm user theo email
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_EXISTED));

        // 4. Mã hóa mật khẩu và lưu DB
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));

        try {
            userRepo.save(user);
        } catch (Exception e) {
            // Ném lỗi 1025 nếu có vấn đề khi lưu xuống DB
            throw new AppException(ErrorCode.RESET_PASSWORD_FAILED);
        }

        // 5. Xóa Token sau khi đổi mật khẩu thành công
        redisTemplate.delete(RESET_TOKEN_PREFIX + request.getResetToken());
    }
}