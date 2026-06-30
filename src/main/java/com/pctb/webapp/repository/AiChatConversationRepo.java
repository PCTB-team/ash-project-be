package com.pctb.webapp.repository;

import com.pctb.webapp.entity.AiChatConversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiChatConversationRepo extends JpaRepository<AiChatConversation, String> {
    Page<AiChatConversation> findByUserIdOrderByUpdatedAtDesc(String userId, Pageable pageable);

    Optional<AiChatConversation> findByIdAndUserId(String id, String userId);
}
