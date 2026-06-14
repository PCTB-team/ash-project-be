package com.pctb.webapp.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Request gui mot cau hoi cua user vao chat session.
 */
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SendAiMessageRequest {
    // Cau hoi user nhap tren chatbox.
    @NotBlank(message = "REQUEST_BODY_INVALID")
    @Size(max = 4000, message = "REQUEST_BODY_INVALID")
    String message;
}
