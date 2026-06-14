package com.pctb.webapp.ai.dto.response;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Response tra ve thong tin chat session cho FE/Swagger.
 */
@Getter
@Setter
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiChatSessionResponse {
    // Id session dung de gui message tiep theo.
    String sessionId;

    // Scope session dang hoi.
    String scopeType;

    // Document id neu chat theo document.
    String documentId;

    // Folder id neu chat theo folder.
    String folderId;

    // Group id neu chat theo group.
    String groupId;

    // Tieu de chat.
    String title;

    // Thoi diem tao.
    String createdAt;

    // Thoi diem cap nhat gan nhat.
    String updatedAt;
}
