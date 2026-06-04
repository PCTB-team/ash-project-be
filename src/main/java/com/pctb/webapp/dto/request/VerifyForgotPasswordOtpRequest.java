package com.pctb.webapp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VerifyForgotPasswordOtpRequest {
    @NotBlank(message = "EMAIL_INVALID")
    @Email(message = "EMAIL_INVALID")
    @Size(max = 100, message = "EMAIL_INVALID")
    String email;

    @NotBlank(message = "FORGOT_PASSWORD_OTP_INVALID")
    @Pattern(regexp = "^\\d{6}$", message = "FORGOT_PASSWORD_OTP_INVALID")
    String otp;
}
