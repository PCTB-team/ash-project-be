package com.pctb.webapp.dto.request;

import jakarta.validation.constraints.Email;
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

    @Size(min = 3, max = 20, message = "USERNAME_INVALID")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "USERNAME_INVALID")
    String username;
    @Email(message = "EMAIL_INVALID")
    String email;

    String fullname;
    @Size(min = 8, message = "PASSWORD_INVALID")
    @Pattern(regexp = ".*[^a-zA-Z0-9].*", message = "PASSWORD_INVALID")
    String password;
    String confirmPassword;
}
