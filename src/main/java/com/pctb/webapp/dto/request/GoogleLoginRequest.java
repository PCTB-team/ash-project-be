package com.pctb.webapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@SuppressWarnings("unused") // Thêm dòng này để tắt cảnh báo "never used" của IntelliJ
public class GoogleLoginRequest {
    @NotBlank(message = "Google ID Token is required")
    private String token;
}