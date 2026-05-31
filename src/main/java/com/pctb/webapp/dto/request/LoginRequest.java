package com.pctb.webapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginRequest {

    @NotBlank(message = "IDENTIFIER_REQUIRED")
    String identifier;

    @NotBlank(message = "PASSWORD_REQUIRED")
    String password;
}
