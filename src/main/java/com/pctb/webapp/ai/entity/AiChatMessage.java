package com.pctb.webapp.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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

/**
 * Luu tung tin nhan trong mot AI chat session.
 * Moi cau hoi cua user va cau tra loi cua AI deu la mot record.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ai_chat_message")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiChatMessage {
    // Id cua message.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    // Session chua message nay.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    AiChatSession session;

    // USER la cau hoi, ASSISTANT la cau tra loi.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    MessageRole role;

    // Noi dung message.
    @Column(columnDefinition = "TEXT", nullable = false)
    String content;

    // Ten model da tra loi, hien tai MVP la fake-llm.
    String modelName;

    // So token prompt, de sau nay tinh cost neu gan AI that.
    Integer promptTokens;

    // So token output, de sau nay tinh cost neu gan AI that.
    Integer completionTokens;

    // Thoi diem tao message.
    @Column(nullable = false)
    LocalDateTime createdAt;
}
