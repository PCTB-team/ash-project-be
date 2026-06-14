package com.pctb.webapp.ai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
 * Luu mot doan text nho cat ra tu document/group file.
 * Retrieval se tim tren bang nay thay vi doc lai file goc moi lan user hoi.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "document_chunk", indexes = {
        @Index(name = "idx_chunk_document", columnList = "document_id"),
        @Index(name = "idx_chunk_group_file", columnList = "group_file_id"),
        @Index(name = "idx_chunk_owner", columnList = "owner_id"),
        @Index(name = "idx_chunk_folder", columnList = "folder_id"),
        @Index(name = "idx_chunk_group", columnList = "group_id")
})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentChunk {
    // Id cua chunk.
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    // Cho biet chunk den tu Document hay GroupFile.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ChunkSourceType sourceType;

    // Id document ca nhan neu sourceType = PERSONAL_DOCUMENT.
    String documentId;

    // Id group file neu sourceType = GROUP_FILE.
    String groupFileId;

    // User so huu/upload file, dung de filter permission.
    String ownerId;

    // Folder cua document ca nhan, dung cho chat theo folder.
    String folderId;

    // Group cua file, dung cho chat theo group.
    String groupId;

    // Thu tu chunk trong file goc.
    @Column(nullable = false)
    Integer chunkIndex;

    // Noi dung day du cua chunk.
    @Column(columnDefinition = "TEXT", nullable = false)
    String content;

    // Doan ngan de tra ve citation cho FE.
    @Column(columnDefinition = "TEXT")
    String contentPreview;

    // Trang trong file neu parser lay duoc, MVP tam null.
    Integer pageNumber;

    // Uoc luong token de sau nay gioi han context.
    Integer tokenCount;

    // Embedding dang JSON, MVP dang [] vi chua gan model embedding that.
    @Column(columnDefinition = "TEXT")
    String embeddingJson;

    // Ten model embedding, MVP la noop.
    String embeddingModel;

    // Thoi diem chunk duoc ingest.
    @Column(nullable = false)
    LocalDateTime ingestedAt;

    // Soft delete chunk khi can an khoi retrieval.
    @Builder.Default
    @Column(nullable = false)
    Boolean deleted = false;
}
