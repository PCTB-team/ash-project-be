package com.pctb.webapp.ai.entity;

import com.pctb.webapp.entity.User;
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
 * Luu mot cuoc chat AI cua user.
 * Session giu scope de backend biet user dang hoi document, folder, library hay group.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ai_chat_session")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiChatSession {
    // Id cua chat session.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    // User tao chat session nay.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    User owner;

    // Scope quyet dinh retrieval se lay chunk tu dau.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ChatScopeType scopeType;

    // Dung khi scopeType = PERSONAL_DOCUMENT.
    String documentId;

    // Dung khi scopeType = PERSONAL_FOLDER.
    String folderId;

    // Dung khi scopeType = GROUP_DOCUMENTS.
    String groupId;

    // Tieu de hien thi cho cuoc chat.
    @Column(nullable = false)
    String title;

    // Thoi diem tao session.
    @Column(nullable = false)
    LocalDateTime createdAt;

    // Thoi diem cap nhat gan nhat.
    LocalDateTime updatedAt;

    // Soft delete de giu lich su neu can khoi phuc/kiem tra sau nay.
    @Builder.Default
    @Column(nullable = false)
    Boolean deleted = false;
}
