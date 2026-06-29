package com.pctb.webapp.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminAiConversationResponse {
    String id;
    String username;
    String type;
    String lastMessage;
    LocalDateTime createdAt;
}
