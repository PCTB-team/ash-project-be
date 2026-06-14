package com.pctb.webapp.ai.repository;

import com.pctb.webapp.ai.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository thao tac voi cac text chunks da ingest tu tai lieu.
 */
public interface DocumentChunkRepo extends JpaRepository<DocumentChunk, String> {
    /**
     * Lay chunks cua mot document ca nhan, dung cho scope PERSONAL_DOCUMENT.
     */
    List<DocumentChunk> findByDocumentIdAndDeletedFalseOrderByChunkIndexAsc(String documentId);

    /**
     * Lay chunks cua toan bo tai lieu ca nhan cua user, dung cho scope PERSONAL_LIBRARY.
     */
    List<DocumentChunk> findByOwnerIdAndDeletedFalse(String ownerId);

    /**
     * Lay chunks trong mot folder ca nhan, dung cho scope PERSONAL_FOLDER.
     */
    List<DocumentChunk> findByOwnerIdAndFolderIdAndDeletedFalse(String ownerId, String folderId);

    /**
     * Lay chunks cua mot group, dung cho scope GROUP_DOCUMENTS.
     */
    List<DocumentChunk> findByGroupIdAndDeletedFalse(String groupId);

    /**
     * Xoa chunks cu cua document truoc khi reindex de tranh trung lap.
     */
    void deleteByDocumentId(String documentId);

    /**
     * Xoa chunks cu cua group file truoc khi reindex de tranh trung lap.
     */
    void deleteByGroupFileId(String groupFileId);
}
