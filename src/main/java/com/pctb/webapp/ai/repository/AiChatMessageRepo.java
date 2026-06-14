package com.pctb.webapp.ai.repository;

import com.pctb.webapp.ai.entity.AiChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository thao tac voi bang ai_chat_message.
 */
public interface AiChatMessageRepo extends JpaRepository<AiChatMessage, String> {
    /**
     * Lay toan bo message cua mot session theo thu tu thoi gian de hien lich su chat.
     */
    List<AiChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}
