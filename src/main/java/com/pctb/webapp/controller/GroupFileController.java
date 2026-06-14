package com.pctb.webapp.controller;

import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.GroupFileResponse;
import com.pctb.webapp.service.GroupFileService;
import io.swagger.v3.oas.annotations.Operation;
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
     * Member uploads a file to a group.
     */
    @Operation(summary = "Upload file to group")
    @PostMapping(value = "/{groupId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<GroupFileResponse> uploadFile(
            @PathVariable String groupId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "replaceExisting", defaultValue = "false") Boolean replaceExisting,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<GroupFileResponse>builder()
                .message("Upload group file successfully")
                .result(groupFileService.uploadFile(groupId, file, replaceExisting, authentication))
                .build();
    }

    /**
     * Members can view active group files.
     */
    @Operation(summary = "Get group files")
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
     * Leader moves a group file to trash.
     */
    @Operation(summary = "Move group file to trash")
    @DeleteMapping("/{groupId}/files/{fileId}")
    public ApiResponse<String> moveFileToTrash(
            @PathVariable String groupId,
            @PathVariable String fileId,
            JwtAuthenticationToken authentication
    ) {
        groupFileService.moveFileToTrash(groupId, fileId, authentication);

        return ApiResponse.<String>builder()
                .message("Move group file to trash successfully")
                .result("DELETED")
                .build();
    }

    /**
     * Leader views trashed group files.
     */
    @Operation(summary = "Get group trash files")
    @GetMapping("/{groupId}/trash/files")
    public ApiResponse<List<GroupFileResponse>> getTrashFiles(
            @PathVariable String groupId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<List<GroupFileResponse>>builder()
                .message("Get group trash files successfully")
                .result(groupFileService.getTrashFiles(groupId, authentication))
                .build();
    }

    /**
     * Leader restores a group file from trash.
     */
    @Operation(summary = "Restore group file from trash")
    @PutMapping("/{groupId}/files/{fileId}/restore")
    public ApiResponse<GroupFileResponse> restoreFile(
            @PathVariable String groupId,
            @PathVariable String fileId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<GroupFileResponse>builder()
                .message("Restore group file successfully")
                .result(groupFileService.restoreFile(groupId, fileId, authentication))
                .build();
    }
}
