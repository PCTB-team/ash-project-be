package com.pctb.webapp.ai.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

/**
 * Response sau khi user gui mot cau hoi cho AI.
 */
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SendAiMessageResponse {
    // Session vua nhan message.
    String sessionId;

    // Id message cau tra loi cua assistant.
    String messageId;

    // Cau tra loi AI tra ve.
    String answer;

    // Danh sach nguon/chunk duoc dung de tra loi.
    List<AiCitationResponse> citations;
}
