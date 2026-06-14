package com.pctb.webapp.ai.repository;

import com.pctb.webapp.ai.entity.AiChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository thao tac voi bang ai_chat_session.
 */
public interface AiChatSessionRepo extends JpaRepository<AiChatSession, String> {
    /**
     * Lay danh sach chat session con active cua user de hien o sidebar/chat history.
     */
    List<AiChatSession> findByOwnerIdAndDeletedFalseOrderByUpdatedAtDesc(String ownerId);
}
