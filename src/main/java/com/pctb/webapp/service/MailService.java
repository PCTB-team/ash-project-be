package com.pctb.webapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MailService {

    static final URI BREVO_SEND_EMAIL_URI = URI.create("https://api.brevo.com/v3/smtp/email");

    final ObjectMapper objectMapper;
    final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${brevo.api-key:}")
    String brevoApiKey;

    @Value("${app.mail.from-email:}")
    String mailFromEmail;

    @Value("${app.mail.from-name:ASH Project}")
    String mailFromName;

    public void sendOtp(String email, String otp) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            throw new RuntimeException("BREVO_API_KEY is not configured");
        }
        if (mailFromEmail == null || mailFromEmail.isBlank()) {
            throw new RuntimeException("MAIL_FROM_EMAIL is not configured");
        }

        Map<String, Object> payload = Map.of(
                "sender", Map.of(
                        "email", mailFromEmail,
                        "name", mailFromName
                ),
                "to", List.of(Map.of("email", email)),
                "subject", "ASH Project - Email verification OTP",
                "htmlContent", """
                        <html>
                            <body>
                                <p>Your OTP code is: <strong>%s</strong></p>
                                <p>This code will expire in 5 minutes.</p>
                                <p>If you did not request this code, please ignore this email.</p>
                            </body>
                        </html>
                        """.formatted(otp)
        );

        HttpRequest request = HttpRequest.newBuilder(BREVO_SEND_EMAIL_URI)
                .header("accept", "application/json")
                .header("api-key", brevoApiKey)
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(toJson(payload)))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException("Failed to send OTP email via Brevo: "
                        + response.statusCode() + " " + response.body());
            }
        } catch (IOException exception) {
            throw new RuntimeException("Failed to connect to Brevo email API", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while sending OTP email via Brevo", exception);
        }
    }

    private String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Failed to build Brevo email payload", exception);
        }
    }
}
