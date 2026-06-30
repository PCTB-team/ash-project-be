package com.pctb.webapp.repository;

import com.pctb.webapp.entity.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiChatMessageRepo extends JpaRepository<AiChatMessage, String> {
    List<AiChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);
}
