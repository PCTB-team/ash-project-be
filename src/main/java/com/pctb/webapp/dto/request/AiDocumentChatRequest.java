package com.pctb.webapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiDocumentChatRequest {
    @NotBlank(message = "AI_DOCUMENT_ID_REQUIRED")
    String documentId;

    @NotBlank(message = "AI_MESSAGE_REQUIRED")
    @Size(max = 4000, message = "AI_MESSAGE_TOO_LONG")
    String message;
}
