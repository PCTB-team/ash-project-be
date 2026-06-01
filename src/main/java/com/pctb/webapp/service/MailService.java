package com.pctb.webapp.service;

import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import lombok.experimental.FieldDefaults;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)

// ================= Class tạo các service của mail ===========================
public class MailService {
    JavaMailSender javaMailSender;

    // Phương thức gửi otp qua email, truyền vào email được gửi, và mã otp
    public void sendOtp(String email,String otp){
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
