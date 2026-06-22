package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.SendGroupMessageRequest;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.GroupMessagePageResponse;
import com.pctb.webapp.dto.response.GroupMessageResponse;
import com.pctb.webapp.service.GroupMessageService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/groups/{groupId}/messages")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupMessageController {
    GroupMessageService groupMessageService;

    /**
     * Lay lich su chat trong group.
     */
    @Operation(summary = "Get group chat messages")
    @GetMapping
    public ApiResponse<GroupMessagePageResponse> getMessages(
            @PathVariable String groupId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<GroupMessagePageResponse>builder()
                .message("Get group messages successfully")
                .result(groupMessageService.getMessages(groupId, page, size, authentication))
                .build();
    }

    /**
     * Gui tin nhan moi vao group.
     */
    @Operation(summary = "Send group chat message")
    @PostMapping
    public ApiResponse<GroupMessageResponse> sendMessage(
            @PathVariable String groupId,
            @RequestBody @Valid SendGroupMessageRequest request,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<GroupMessageResponse>builder()
                .message("Send group message successfully")
                .result(groupMessageService.sendMessage(groupId, request, authentication))
                .build();
    }
}
