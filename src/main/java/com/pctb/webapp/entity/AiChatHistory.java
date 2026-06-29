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
        name = "ai_chat_history",
        indexes = @Index(
                name = "idx_ai_chat_history_user_created_at",
                columnList = "user_id, created_at"
        )
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiChatHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User user;

    @Column(name = "document_id", length = 100)
    String documentId;

    @Column(name = "folder_id", length = 100)
    String folderId;

    @Column(nullable = false, length = 4000)
    String question;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    String answer;

    @Column(name = "answer_source", length = 30)
    String answerSource;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;
}
