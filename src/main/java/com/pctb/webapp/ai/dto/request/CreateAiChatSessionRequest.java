package com.pctb.webapp.ai.dto.request;

import com.pctb.webapp.ai.entity.ChatScopeType;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Request tao mot AI chat session moi.
 * FE gui scopeType kem documentId/folderId/groupId tuy man hinh dang chat.
 */
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateAiChatSessionRequest {
    // Loai scope bat buoc: document, folder, library hoac group.
    @NotNull(message = "REQUEST_BODY_INVALID")
    ChatScopeType scopeType;

    // Dung khi scopeType = PERSONAL_DOCUMENT.
    String documentId;

    // Dung khi scopeType = PERSONAL_FOLDER.
    String folderId;

    // Dung khi scopeType = GROUP_DOCUMENTS.
    String groupId;
}
