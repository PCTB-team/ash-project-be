package com.pctb.webapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LockUserRequest {
    @NotBlank(message = "Lock reason must not be blank")
    private String reason;
}