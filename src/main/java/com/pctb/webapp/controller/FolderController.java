package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.CreateFolderRequest;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.FolderResponse;
import com.pctb.webapp.service.FolderService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/folders")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FolderController {
    FolderService folderService;

    @Operation(summary = "Create folder")
    @PostMapping
    public ApiResponse<FolderResponse> createFolder(
            @RequestBody @Valid CreateFolderRequest request,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<FolderResponse>builder()
                .message("Create folder successfully")
                .result(folderService.createFolder(request, authentication))
                .build();
    }

    @Operation(summary = "Get current user folders")
    @GetMapping
    public ApiResponse<List<FolderResponse>> getMyFolders(
            @RequestParam(value = "parentFolderId", required = false) String parentFolderId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<List<FolderResponse>>builder()
                .message("Get folders successfully")
                .result(folderService.getMyFolders(parentFolderId, authentication))
                .build();
    }

    @Operation(summary = "Move current user folder to trash")
    @DeleteMapping("/{folderId}")
    public ApiResponse<FolderResponse> deleteMyFolder(
            @PathVariable String folderId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<FolderResponse>builder()
                .message("Delete folder successfully")
                .result(folderService.deleteMyFolder(folderId, authentication))
                .build();
    }

    @Operation(summary = "Restore current user folder from trash")
    @PutMapping("/{folderId}/restore")
    public ApiResponse<FolderResponse> restoreMyFolder(
            @PathVariable String folderId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<FolderResponse>builder()
                .message("Restore folder successfully")
                .result(folderService.restoreMyFolder(folderId, authentication))
                .build();
    }

    @Operation(summary = "Delete current user folder permanently")
    @DeleteMapping("/{folderId}/permanent")
    public ApiResponse<FolderResponse> deleteMyFolderPermanently(
            @PathVariable String folderId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<FolderResponse>builder()
                .message("Delete folder permanently successfully")
                .result(folderService.deleteMyFolderPermanently(folderId, authentication))
                .build();
    }
}
