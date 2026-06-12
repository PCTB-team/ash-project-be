package com.pctb.webapp.ai.controller;

import com.pctb.webapp.ai.dto.response.IndexStatusResponse;
import com.pctb.webapp.ai.service.AiPermissionService;
import com.pctb.webapp.ai.service.DocumentIngestionService;
import com.pctb.webapp.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API kiem tra va chay lai qua trinh ingest tai lieu cho AI.
 * Ingest = doc file, extract text, cat chunk va luu vao bang document_chunk.
 */
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AiIngestionController {
    DocumentIngestionService ingestionService;

    AiPermissionService permissionService;

    /**
     * Chay lai index cho document ca nhan.
     * Endpoint nay huu ich khi upload cu bi FAILED hoac can refresh chunks.
     */
    @PostMapping("/documents/{documentId}/reindex")
    public ApiResponse<IndexStatusResponse> reindexDocument(
            @PathVariable String documentId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<IndexStatusResponse>builder()
                .message("Document reindex completed successfully")
                .result(ingestionService.ingestPersonalDocument(documentId, authentication.getName()))
                .build();
    }

    /**
     * Xem document ca nhan da san sang chat chua.
     * COMPLETED nghia la co chunks de retrieval.
     */
    @GetMapping("/documents/{documentId}/index-status")
    public ApiResponse<IndexStatusResponse> getDocumentIndexStatus(
            @PathVariable String documentId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<IndexStatusResponse>builder()
                .message("Get document index status successfully")
                .result(ingestionService.getPersonalDocumentStatus(documentId, authentication.getName()))
                .build();
    }

    /**
     * Chay lai index cho file trong group.
     * Chi approved member cua group moi duoc goi.
     */
    @PostMapping("/groups/{groupId}/documents/{groupFileId}/reindex")
    public ApiResponse<IndexStatusResponse> reindexGroupFile(
            @PathVariable String groupId,
            @PathVariable String groupFileId,
            JwtAuthenticationToken authentication
    ) {
        permissionService.requireApprovedGroupMember(groupId, authentication.getName());

        return ApiResponse.<IndexStatusResponse>builder()
                .message("Group document reindex completed successfully")
                .result(ingestionService.ingestGroupFile(groupId, groupFileId))
                .build();
    }

    /**
     * Xem trang thai index cua file trong group.
     * Chi approved member cua group moi duoc xem.
     */
    @GetMapping("/groups/{groupId}/documents/{groupFileId}/index-status")
    public ApiResponse<IndexStatusResponse> getGroupFileIndexStatus(
            @PathVariable String groupId,
            @PathVariable String groupFileId,
            JwtAuthenticationToken authentication
    ) {
        permissionService.requireApprovedGroupMember(groupId, authentication.getName());

        return ApiResponse.<IndexStatusResponse>builder()
                .message("Get group document index status successfully")
                .result(ingestionService.getGroupFileStatus(groupId, groupFileId))
                .build();
    }
}
