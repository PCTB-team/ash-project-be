package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.UpdateDocumentRequest;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.DeleteDocumentResponse;
import com.pctb.webapp.dto.response.DocumentResponse;
import com.pctb.webapp.dto.response.DownloadDocumentResponse;
import jakarta.validation.Valid;
import com.pctb.webapp.service.DocumentService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentController {
    DocumentService documentService;

    @GetMapping
    public ApiResponse<List<DocumentResponse>> getMyDocuments(JwtAuthenticationToken authentication) {
        return ApiResponse.<List<DocumentResponse>>builder()
                .message("Get documents successfully")
                .result(documentService.getMyDocuments(authentication))
                .build();
    }

    @GetMapping("/trash")
    public ApiResponse<List<DocumentResponse>> getMyTrashDocuments(JwtAuthenticationToken authentication) {
        return ApiResponse.<List<DocumentResponse>>builder()
                .message("Get trash documents successfully")
                .result(documentService.getMyTrashDocuments(authentication))
                .build();
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> downloadMyDocument(
            @PathVariable String documentId,
            JwtAuthenticationToken authentication
    ) {
        DownloadDocumentResponse response = documentService.downloadMyDocument(documentId, authentication);

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

    @DeleteMapping("/{documentId}")
    public ApiResponse<DeleteDocumentResponse> deleteMyDocument(
            @PathVariable String documentId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<DeleteDocumentResponse>builder()
                .message("Delete document successfully")
                .result(documentService.deleteMyDocument(documentId, authentication))
                .build();
    }

    @PutMapping("/{documentId}/restore")
    public ApiResponse<DocumentResponse> restoreMyDocument(
            @PathVariable String documentId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<DocumentResponse>builder()
                .message("Restore document successfully")
                .result(documentService.restoreMyDocument(documentId, authentication))
                .build();
    }

    @DeleteMapping("/{documentId}/permanent")
    public ApiResponse<DeleteDocumentResponse> deleteMyDocumentPermanently(
            @PathVariable String documentId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<DeleteDocumentResponse>builder()
                .message("Delete document permanently successfully")
                .result(documentService.deleteMyDocumentPermanently(documentId, authentication))
                .build();
    }

    @PutMapping("/{documentId}")
    public ApiResponse<DocumentResponse> updateMyDocument(
            @PathVariable String documentId,
            @RequestBody @Valid UpdateDocumentRequest request,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<DocumentResponse>builder()
                .message("Update document successfully")
                .result(documentService.updateMyDocument(documentId, request, authentication))
                .build();
    }
}
