package com.pctb.webapp.controller;

import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.GroupFileResponse;
import com.pctb.webapp.service.GroupFileService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupFileController {
    GroupFileService groupFileService;

    /**
     * Member upload file vao group.
     * Backend se check member APPROVED va canUpload=true truoc khi luu file.
     */
    @PostMapping(value = "/{groupId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<GroupFileResponse> uploadFile(
            @PathVariable String groupId,
            @RequestParam("file") MultipartFile file,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<GroupFileResponse>builder()
                .message("Upload group file successfully")
                .result(groupFileService.uploadFile(groupId, file, authentication))
                .build();
    }

    /**
     * Member da APPROVED xem danh sach file trong group.
     */
    @GetMapping("/{groupId}/files")
    public ApiResponse<List<GroupFileResponse>> getGroupFiles(
            @PathVariable String groupId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<List<GroupFileResponse>>builder()
                .message("Get group files successfully")
                .result(groupFileService.getGroupFiles(groupId, authentication))
                .build();
    }

    /**
     * Member upload document vao group.
     * Route nay dung theo design moi, logic giong /files.
     */
    @PostMapping(value = "/{groupId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<GroupFileResponse> uploadDocument(
            @PathVariable String groupId,
            @RequestParam("file") MultipartFile file,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<GroupFileResponse>builder()
                .message("Upload group document successfully")
                .result(groupFileService.uploadDocument(groupId, file, authentication))
                .build();
    }

    /**
     * Member da APPROVED xem document active trong group.
     */
    @GetMapping("/{groupId}/documents")
    public ApiResponse<List<GroupFileResponse>> getActiveDocuments(
            @PathVariable String groupId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<List<GroupFileResponse>>builder()
                .message("Get group documents successfully")
                .result(groupFileService.getActiveDocuments(groupId, authentication))
                .build();
    }

    /**
     * Leader dua document vao trash.
     */
    @DeleteMapping("/{groupId}/documents/{documentId}")
    public ApiResponse<String> moveDocumentToTrash(
            @PathVariable String groupId,
            @PathVariable String documentId,
            JwtAuthenticationToken authentication
    ) {
        groupFileService.moveDocumentToTrash(groupId, documentId, authentication);

        return ApiResponse.<String>builder()
                .message("Move group document to trash successfully")
                .result("DELETED")
                .build();
    }

    /**
     * Leader xem document trong trash cua group.
     */
    @GetMapping("/{groupId}/trash/documents")
    public ApiResponse<List<GroupFileResponse>> getTrashDocuments(
            @PathVariable String groupId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<List<GroupFileResponse>>builder()
                .message("Get group trash documents successfully")
                .result(groupFileService.getTrashDocuments(groupId, authentication))
                .build();
    }

    /**
     * Leader restore document tu trash ve danh sach active.
     */
    @PutMapping("/{groupId}/documents/{documentId}/restore")
    public ApiResponse<GroupFileResponse> restoreDocument(
            @PathVariable String groupId,
            @PathVariable String documentId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<GroupFileResponse>builder()
                .message("Restore group document successfully")
                .result(groupFileService.restoreDocument(groupId, documentId, authentication))
                .build();
    }
}
