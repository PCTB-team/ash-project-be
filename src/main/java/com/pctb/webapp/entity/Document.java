package com.pctb.webapp.entity;

import com.pctb.webapp.exception.UploadStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "document", indexes = {
        @Index(name = "idx_document_owner_deleted_created", columnList = "user_id, deleted, created_at"),
        @Index(name = "idx_document_owner_file_name", columnList = "user_id, file_name"),
        @Index(name = "idx_document_owner_folder_deleted_created", columnList = "user_id, folder_id, deleted, created_at")
})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Document {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(nullable = false)
    String title;

    @Column(nullable = false)
    String fileName;

    @Column(nullable = false)
    String fileExtension;

    @Column(nullable = false)
    String mimeType;

    @Column(nullable = false)
    Long fileSize;

    @Column(nullable = false)
    String storageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    UploadStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id")
    StudyGroup group;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    Folder folder;

    @Column(nullable = false)
    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    @Builder.Default
    Boolean deleted = false;

    LocalDateTime deletedAt;
}