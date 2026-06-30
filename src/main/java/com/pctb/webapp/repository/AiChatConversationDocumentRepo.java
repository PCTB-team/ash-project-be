package com.pctb.webapp.repository;

import com.pctb.webapp.entity.AiChatConversationDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiChatConversationDocumentRepo extends JpaRepository<AiChatConversationDocument, String> {
    boolean existsByConversationIdAndDocumentId(String conversationId, String documentId);

    List<AiChatConversationDocument> findByConversationId(String conversationId);
}
