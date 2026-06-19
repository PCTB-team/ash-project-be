package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.UpdateDocumentRequest;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.DeleteDocumentResponse;
import com.pctb.webapp.dto.response.DocumentPreviewResponse;
import com.pctb.webapp.dto.response.DocumentResponse;
import com.pctb.webapp.dto.response.DownloadDocumentResponse;
import com.pctb.webapp.dto.response.FileSystemItemResponse;
import com.pctb.webapp.dto.response.FilteredDocumentResponse;
import com.pctb.webapp.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentController {
    DocumentService documentService;

    @Operation(summary = "Get current user documents")
    @GetMapping
    public ApiResponse<List<DocumentResponse>> getMyDocuments(JwtAuthenticationToken authentication) {
        return ApiResponse.<List<DocumentResponse>>builder()
                .message("Get documents successfully")
                .result(documentService.getMyDocuments(authentication))
                .build();
    }

    @Operation(summary = "Get current user documents by page")
    @GetMapping("/page")
    public ApiResponse<Page<DocumentResponse>> getMyDocumentsPage(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "folderId", required = false) String folderId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<Page<DocumentResponse>>builder()
                .message("Get documents successfully")
                .result(documentService.getMyDocumentsPage(authentication, page, size, folderId))
                .build();
    }

    @Operation(summary = "Get current user files and folders by page")
    @GetMapping("/items")
    public ApiResponse<Page<FileSystemItemResponse>> getMyFileSystemItemsPage(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "folderId", required = false) String folderId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<Page<FileSystemItemResponse>>builder()
                .message("Get files and folders successfully")
                .result(documentService.getMyFileSystemItemsPage(authentication, page, size, folderId))
                .build();
    }

    @Operation(summary = "Search current user documents by file name")
    @GetMapping("/search")
    public ApiResponse<Page<DocumentResponse>> searchMyDocuments(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "folderId", required = false) String folderId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<Page<DocumentResponse>>builder()
                .message("Search documents successfully")
                .result(documentService.searchMyDocuments(authentication, keyword, page, size, folderId))
                .build();
    }

    @Operation(summary = "Filter current user documents by file type")
    @GetMapping("/filter")
    public ApiResponse<FilteredDocumentResponse> filterMyDocumentsByFileType(
            @RequestParam("fileType") String fileType,
            @RequestParam(value = "folderId", required = false) String folderId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<FilteredDocumentResponse>builder()
                .message("Filter documents successfully")
                .result(documentService.filterMyDocumentsByFileType(authentication, fileType, folderId))
                .build();
    }

    @Operation(summary = "Get current user trash documents")
    @GetMapping("/filter/documents")
    public ApiResponse<FilteredDocumentResponse> filterMyDocumentFiles(
            @RequestParam(value = "folderId", required = false) String folderId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<FilteredDocumentResponse>builder()
                .message("Filter document files successfully")
                .result(documentService.filterMyDocumentFiles(authentication, folderId))
                .build();
    }

    @GetMapping("/trash")
    public ApiResponse<List<DocumentResponse>> getMyTrashDocuments(JwtAuthenticationToken authentication) {
        return ApiResponse.<List<DocumentResponse>>builder()
                .message("Get trash documents successfully")
                .result(documentService.getMyTrashDocuments(authentication))
                .build();
    }

    @Operation(summary = "Download current user document")
    @GetMapping("/{documentId}/download")
    public ResponseEntity<Resource> downloadMyDocument(
            @PathVariable String documentId,
            JwtAuthenticationToken authentication
    ) {
        DownloadDocumentResponse response = documentService.downloadMyDocument(documentId, authentication);

        return buildFileResponse(response, ContentDisposition.attachment());
    }

    @Operation(summary = "View current user document")
    @GetMapping("/{documentId}/view")
    public ResponseEntity<Resource> viewMyDocument(
            @PathVariable String documentId,
            JwtAuthenticationToken authentication
    ) {
        DownloadDocumentResponse response = documentService.downloadMyDocument(documentId, authentication);

        return buildFileResponse(response, ContentDisposition.inline());
    }

    @Operation(summary = "Get current user document preview URL")
    @GetMapping("/{documentId}/preview")
    public ApiResponse<DocumentPreviewResponse> getMyDocumentPreview(
            @PathVariable String documentId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<DocumentPreviewResponse>builder()
                .message("Get document preview successfully")
                .result(documentService.getMyDocumentPreview(documentId, authentication))
                .build();
    }

    @Operation(summary = "Move current user document to trash")
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

    @Operation(summary = "Restore current user document from trash")
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

    @Operation(summary = "Delete current user document permanently")
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

    @Operation(summary = "Update current user document metadata")
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

    private ResponseEntity<Resource> buildFileResponse(
            DownloadDocumentResponse response,
            ContentDisposition.Builder contentDispositionBuilder
    ) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(response.getMimeType()))
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        contentDispositionBuilder
                                .filename(response.getFileName())
                                .build()
                                .toString()
                )
                .body(response.getResource());
    }
}
