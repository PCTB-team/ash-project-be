package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.DeleteTrashItemsRequest;
import com.pctb.webapp.dto.request.RestoreTrashItemsRequest;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.DeleteTrashItemsResponse;
import com.pctb.webapp.dto.response.RestoreTrashItemsResponse;
import com.pctb.webapp.dto.response.TrashResponse;
import com.pctb.webapp.service.TrashService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trash")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TrashController {
    TrashService trashService;

    @Operation(summary = "Get current user trash items")
    @GetMapping
    public ApiResponse<TrashResponse> getMyTrash(JwtAuthenticationToken authentication) {
        return ApiResponse.<TrashResponse>builder()
                .message("Get trash successfully")
                .result(trashService.getMyTrash(authentication))
                .build();
    }

    @Operation(summary = "Delete selected trash items permanently")
    @DeleteMapping("/items")
    public ApiResponse<DeleteTrashItemsResponse> deleteTrashItems(
            @RequestBody DeleteTrashItemsRequest request,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<DeleteTrashItemsResponse>builder()
                .message("Delete trash items successfully")
                .result(trashService.deleteTrashItems(request, authentication))
                .build();
    }

    @Operation(summary = "Restore selected trash items")
    @PutMapping("/items/restore")
    public ApiResponse<RestoreTrashItemsResponse> restoreTrashItems(
            @RequestBody RestoreTrashItemsRequest request,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<RestoreTrashItemsResponse>builder()
                .message("Restore trash items successfully")
                .result(trashService.restoreTrashItems(request, authentication))
                .build();
    }
}
