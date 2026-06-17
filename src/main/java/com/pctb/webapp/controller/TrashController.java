package com.pctb.webapp.controller;

import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.TrashResponse;
import com.pctb.webapp.service.TrashService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
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
}
