package com.pctb.webapp.controller;


import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.DocumentResponse;
import com.pctb.webapp.dto.response.DownloadDocumentResponse;
import com.pctb.webapp.service.GroupDocumentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.pctb.webapp.dto.response.DeleteDocumentResponse;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupDocumentController {
    GroupDocumentService groupDocumentService;

    // Member upload file khi co quyen.
    @PostMapping(value = "/{groupId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DocumentResponse> uploadDocument(
            @PathVariable String groupId,
            @RequestParam("file") MultipartFile file,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<DocumentResponse>builder()
                .message("Upload group document successfully")
                .result(groupDocumentService.uploadDocument(groupId, file, authentication))
                .build();
    }

    // Member xem file trong group.
    @GetMapping("/{groupId}/documents")
    public ApiResponse<List<DocumentResponse>> getGroupDocuments(
            @PathVariable String groupId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<List<DocumentResponse>>builder()
                .message("Get group documents successfully")
                .result(groupDocumentService.getGroupDocuments(groupId, authentication))
                .build();
    }

    // Member tai file trong group.
    @GetMapping("/{groupId}/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadGroupDocument(
            @PathVariable String groupId,
            @PathVariable String documentId,
            JwtAuthenticationToken authentication
    ) {
        DownloadDocumentResponse response =
                groupDocumentService.downloadGroupDocument(groupId, documentId, authentication);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(response.getMimeType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(response.getFileName())
                                .build()
                                .toString()
                )
                .body(response.getResource());
    }

    // Leader xoa file trong group vao thung rac.
    @DeleteMapping("/{groupId}/documents/{documentId}")
    public ApiResponse<DeleteDocumentResponse> deleteGroupDocument(
            @PathVariable String groupId,
            @PathVariable String documentId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<DeleteDocumentResponse>builder()
                .message("Delete group document successfully")
                .result(groupDocumentService.deleteGroupDocument(groupId, documentId, authentication))
                .build();
    }
}
