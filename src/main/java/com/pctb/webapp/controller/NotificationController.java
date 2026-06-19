package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.MarkNotificationReadRequest;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.NotificationListResponse;
import com.pctb.webapp.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationController {
    NotificationService notificationService;

    @Operation(summary = "Get my notifications")
    @GetMapping
    public ApiResponse<NotificationListResponse> getMyNotifications(
            @RequestParam(required = false) Boolean read,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<NotificationListResponse>builder()
                .message("Get notifications successfully")
                .result(notificationService.getMyNotifications(read, page, size, authentication))
                .build();
    }

    @Operation(summary = "Mark notifications as read")
    @PutMapping("/read")
    public ApiResponse<NotificationListResponse> markAsRead(
            @RequestBody MarkNotificationReadRequest request,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<NotificationListResponse>builder()
                .message("Mark notifications as read successfully")
                .result(notificationService.markAsRead(request, authentication))
                .build();
    }
}
