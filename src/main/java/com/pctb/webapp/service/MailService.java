package com.pctb.webapp.service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
// Service phụ trách các thao tác gửi email của hệ thống.
public class MailService {
    JavaMailSender javaMailSender;

    // Gửi mã OTP đến email người dùng để phục vụ xác thực đăng ký hoặc khôi phục mật khẩu.
    public void sendOtp(String email, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("ASH Project - Email verification OTP");
        message.setText("""
                Your OTP code is: %s

                This code will expire in 5 minutes.
                If you did not request this code, please ignore this email.
                """.formatted(otp));

        javaMailSender.send(message);
    }
}
