package com.pctb.webapp.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
// =================== Format Request Khi vào Controller ===============//
public class RegisterRequest {

    @NotBlank(message = "USERNAME_INVALID")
    @Size(min = 3, max = 20, message = "USERNAME_INVALID")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "USERNAME_INVALID")
    String username;

    @NotBlank(message = "EMAIL_INVALID")
    @Email(message = "EMAIL_INVALID")
    String email;

    String fullname;
    @NotBlank(message = "PASSWORD_INVALID")
    @Size(min = 8, message = "PASSWORD_INVALID")
    @Pattern(regexp = ".*[^a-zA-Z0-9].*", message = "PASSWORD_INVALID")
    String password;

    @NotBlank(message = "CONFIRM_PASSWORD_NOT_MATCH")
    String confirmPassword;


}
