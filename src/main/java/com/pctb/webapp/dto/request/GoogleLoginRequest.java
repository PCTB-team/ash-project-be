package com.pctb.webapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@SuppressWarnings("unused") // Thêm dòng này để tắt cảnh báo "never used" của IntelliJ
public class GoogleLoginRequest {
    @NotBlank(message = "GOOGLE_TOKEN_REQUIRED")
    private String token;
}