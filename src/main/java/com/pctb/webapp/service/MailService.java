package com.pctb.webapp.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MailService {

    @Value("${resend.api-key}")
    String resendApiKey;

    @Value("${app.mail.from}")
    String mailFrom;

    public void sendOtp(String email, String otp) {
        Resend resend = new Resend(resendApiKey);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(mailFrom)
                .to(email)
                .subject("ASH Project - Email verification OTP")
                .html("""
                        <p>Your OTP code is: <strong>%s</strong></p>
                        <p>This code will expire in 5 minutes.</p>
                        <p>If you did not request this code, please ignore this email.</p>
                        """.formatted(otp))
                .build();

        try {
            resend.emails().send(params);
        } catch (ResendException exception) {
            throw new RuntimeException("Failed to send OTP email", exception);
        }
    }
}
