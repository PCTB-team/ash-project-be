package com.pctb.webapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pctb.webapp.dto.request.PendingRegisterRequest;
import com.pctb.webapp.dto.request.RegisterRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenService {
    UserRepo userRepo;

    PasswordEncoder passwordEncoder;
    OtpService otpService;
    RedisService redisService;
    ObjectMapper objectMapper;
    @Value("${app.otp.resend-cooldown-seconds}")
    @NonFinal
    long resendCooldownSeconds;

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
                resendCooldownSeconds);

        otpService.sendOtpAfterRegister(request.getEmail());

        return RegisterResponse.builder()
                .fullname(request.getFullname())
                .email(request.getEmail())
                .username(request.getUsername())
                .build();
    }
    private String pendingRegisterKey(String email) {
        return "register:pending:" + email;
    }

}
