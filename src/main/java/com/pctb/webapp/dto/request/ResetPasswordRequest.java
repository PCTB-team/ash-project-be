package com.pctb.webapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    @NotBlank(message = "RESET_TOKEN_INVALID")
    private String resetToken;

    @NotBlank(message = "PASSWORD_INVALID")
    @Size(min = 8, message = "PASSWORD_INVALID")
    @Pattern(regexp = ".*[^a-zA-Z0-9].*", message = "PASSWORD_INVALID")
    private String newPassword;

    @NotBlank(message = "RESET_PASSWORD_MISMATCH")
    private String confirmPassword;
}