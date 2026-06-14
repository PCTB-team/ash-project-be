package com.pctb.webapp.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pctb.webapp.ai.dto.response.IndexStatusResponse;
import com.pctb.webapp.ai.entity.ChunkSourceType;
import com.pctb.webapp.ai.entity.DocumentChunk;
import com.pctb.webapp.ai.entity.IngestionStatus;
import com.pctb.webapp.ai.repository.DocumentChunkRepo;
import com.pctb.webapp.entity.Document;
import com.pctb.webapp.entity.Folder;
import com.pctb.webapp.entity.GroupFile;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.DocumentRepo;
import com.pctb.webapp.repository.GroupFileRepo;
import com.pctb.webapp.service.StorageService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Xu ly ingest tai lieu cho AI.
 * Ingest gom: doc file tu storage, extract text, cat chunk, tao embedding placeholder va luu chunks.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentIngestionService {
    DocumentRepo documentRepo;

    GroupFileRepo groupFileRepo;

    DocumentChunkRepo documentChunkRepo;

    StorageService storageService;

    TextExtractionService textExtractionService;

    ChunkingService chunkingService;

    EmbeddingService embeddingService;

    ObjectMapper objectMapper;

    /**
     * Ingest document ca nhan cua user.
     * Ham nay duoc goi sau upload va cung duoc goi lai bang endpoint reindex.
     */
    @Transactional
    public IndexStatusResponse ingestPersonalDocument(String documentId, String userId) {
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!document.getOwner().getId().equals(userId)) {
            throw new AppException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        if (Boolean.TRUE.equals(document.getDeleted())) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        document.setIngestionStatus(IngestionStatus.PROCESSING);
        document.setIngestionError(null);
        documentRepo.save(document);

        try {
            // Load file vat ly tu storageUrl, parse text, roi cat thanh nhieu chunks nho.
            Resource resource = storageService.loadAsResource(document.getStorageUrl());
            List<String> chunks = chunkingService.chunk(textExtractionService.extract(resource));

            // Xoa chunks cu truoc khi luu chunks moi de reindex khong bi trung.
            documentChunkRepo.deleteByDocumentId(document.getId());

            for (int i = 0; i < chunks.size(); i++) {
                String content = chunks.get(i);
                Folder folder = document.getFolder();

                documentChunkRepo.save(DocumentChunk.builder()
                        .sourceType(ChunkSourceType.PERSONAL_DOCUMENT)
                        .documentId(document.getId())
                        .ownerId(document.getOwner().getId())
                        .folderId(folder == null ? null : folder.getId())
                        .chunkIndex(i)
                        .content(content)
                        .contentPreview(preview(content))
                        .tokenCount(estimateTokenCount(content))
                        .embeddingJson(toJson(embeddingService.embed(content)))
                        .embeddingModel("noop")
                        .ingestedAt(LocalDateTime.now())
                        .deleted(false)
                        .build());
            }

            // Mark COMPLETED de FE biet document da chat duoc.
            document.setIngestionStatus(IngestionStatus.COMPLETED);
            document.setLastIngestedAt(LocalDateTime.now());
            document.setIngestionError(null);
        } catch (RuntimeException exception) {
            // Khong nem loi ra ngoai de upload/reindex co the tra status FAILED ro rang.
            document.setIngestionStatus(IngestionStatus.FAILED);
            document.setIngestionError(exception.getMessage());
        }

        documentRepo.save(document);
        return buildDocumentStatus(document);
    }

    /**
     * Ingest file trong group.
     * Chi xu ly file thuoc dung groupId va chua bi dua vao trash.
     */
    @Transactional
    public IndexStatusResponse ingestGroupFile(String groupId, String groupFileId) {
        GroupFile groupFile = groupFileRepo.findById(groupFileId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_FILE_NOT_FOUND));

        if (!groupFile.getGroup().getId().equals(groupId)) {
            throw new AppException(ErrorCode.GROUP_FILE_NOT_IN_GROUP);
        }

        if (Boolean.TRUE.equals(groupFile.getDeleted())) {
            throw new AppException(ErrorCode.GROUP_FILE_NOT_FOUND);
        }

        groupFile.setIngestionStatus(IngestionStatus.PROCESSING);
        groupFile.setIngestionError(null);
        groupFileRepo.save(groupFile);

        try {
            // Doc file group, parse text, cat chunk va xoa chunks cu neu co.
            Resource resource = storageService.loadAsResource(groupFile.getStorageUrl());
            List<String> chunks = chunkingService.chunk(textExtractionService.extract(resource));
            documentChunkRepo.deleteByGroupFileId(groupFile.getId());

            for (int i = 0; i < chunks.size(); i++) {
                String content = chunks.get(i);

                documentChunkRepo.save(DocumentChunk.builder()
                        .sourceType(ChunkSourceType.GROUP_FILE)
                        .groupFileId(groupFile.getId())
                        .ownerId(groupFile.getUploadedBy().getId())
                        .groupId(groupFile.getGroup().getId())
                        .chunkIndex(i)
                        .content(content)
                        .contentPreview(preview(content))
                        .tokenCount(estimateTokenCount(content))
                        .embeddingJson(toJson(embeddingService.embed(content)))
                        .embeddingModel("noop")
                        .ingestedAt(LocalDateTime.now())
                        .deleted(false)
                        .build());
            }

            // Mark COMPLETED de group member co the chat tren tai lieu nay.
            groupFile.setIngestionStatus(IngestionStatus.COMPLETED);
            groupFile.setLastIngestedAt(LocalDateTime.now());
            groupFile.setIngestionError(null);
        } catch (RuntimeException exception) {
            // Luu loi vao entity de Swagger/FE xem duoc ly do failed.
            groupFile.setIngestionStatus(IngestionStatus.FAILED);
            groupFile.setIngestionError(exception.getMessage());
        }

        groupFileRepo.save(groupFile);
        return buildGroupFileStatus(groupFile);
    }

    /**
     * Lay trang thai ingest cua document ca nhan va dam bao user dung la owner.
     */
    public IndexStatusResponse getPersonalDocumentStatus(String documentId, String userId) {
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!document.getOwner().getId().equals(userId)) {
            throw new AppException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        return buildDocumentStatus(document);
    }

    /**
     * Lay trang thai ingest cua group file va dam bao file thuoc dung group.
     */
    public IndexStatusResponse getGroupFileStatus(String groupId, String groupFileId) {
        GroupFile groupFile = groupFileRepo.findById(groupFileId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_FILE_NOT_FOUND));

        if (!groupFile.getGroup().getId().equals(groupId)) {
            throw new AppException(ErrorCode.GROUP_FILE_NOT_IN_GROUP);
        }

        return buildGroupFileStatus(groupFile);
    }

    /**
     * Map entity Document sang response status cho Swagger/FE.
     */
    private IndexStatusResponse buildDocumentStatus(Document document) {
        return IndexStatusResponse.builder()
                .sourceId(document.getId())
                .sourceType(ChunkSourceType.PERSONAL_DOCUMENT.name())
                .ingestionStatus(document.getIngestionStatus().name())
                .ingestionError(document.getIngestionError())
                .lastIngestedAt(document.getLastIngestedAt() == null ? null : document.getLastIngestedAt().toString())
                .build();
    }

    /**
     * Map entity GroupFile sang response status cho Swagger/FE.
     */
    private IndexStatusResponse buildGroupFileStatus(GroupFile groupFile) {
        return IndexStatusResponse.builder()
                .sourceId(groupFile.getId())
                .sourceType(ChunkSourceType.GROUP_FILE.name())
                .ingestionStatus(groupFile.getIngestionStatus().name())
                .ingestionError(groupFile.getIngestionError())
                .lastIngestedAt(groupFile.getLastIngestedAt() == null ? null : groupFile.getLastIngestedAt().toString())
                .build();
    }

    /**
     * Cat doan preview ngan de FE hien citation.
     */
    private String preview(String content) {
        return content.substring(0, Math.min(content.length(), 300));
    }

    /**
     * Uoc luong token don gian de sau nay co the gioi han prompt context.
     */
    private int estimateTokenCount(String content) {
        return Math.max(1, content.length() / 4);
    }

    /**
     * Convert embedding list sang JSON string de luu DB.
     * MVP dung NoopEmbeddingService nen gia tri thuong la [].
     */
    private String toJson(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }
}
