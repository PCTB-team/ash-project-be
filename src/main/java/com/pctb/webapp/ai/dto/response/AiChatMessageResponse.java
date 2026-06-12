package com.pctb.webapp.ai.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Response mot message trong lich su chat.
 */
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiChatMessageResponse {
    // Id message.
    String messageId;

    // USER hoac ASSISTANT.
    String role;

    // Noi dung message.
    String content;

    // Thoi diem tao message.
    String createdAt;
}
