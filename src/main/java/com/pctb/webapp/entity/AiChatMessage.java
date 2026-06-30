package com.pctb.webapp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "ai_chat_message",
        indexes = @Index(
                name = "idx_ai_chat_message_conversation_created_at",
                columnList = "conversation_id, created_at"
        )
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    AiChatConversation conversation;

    @Column(nullable = false, length = 20)
    String role;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    String content;

    @Column(name = "answer_source", length = 30)
    String answerSource;

    @Lob
    @Column(name = "sources_json", columnDefinition = "TEXT")
    String sourcesJson;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;
}
