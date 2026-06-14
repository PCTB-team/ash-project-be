package com.pctb.webapp.ai.controller;

import com.pctb.webapp.ai.dto.request.CreateAiChatSessionRequest;
import com.pctb.webapp.ai.dto.request.SendAiMessageRequest;
import com.pctb.webapp.ai.dto.response.AiChatMessageResponse;
import com.pctb.webapp.ai.dto.response.AiChatSessionResponse;
import com.pctb.webapp.ai.dto.response.SendAiMessageResponse;
import com.pctb.webapp.ai.service.AiChatService;
import com.pctb.webapp.ai.service.AiChatSessionService;
import com.pctb.webapp.dto.response.ApiResponse;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST API cho AI chatbox.
 * Controller nay chi nhan request/tra response, logic nam trong service.
 */
@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AiChatController {
    AiChatSessionService sessionService;

    AiChatService chatService;

    /**
     * Tao chat session moi theo scope user chon.
     * Vi du: chat theo mot document ca nhan hoac chat theo group documents.
     */
    @PostMapping("/sessions")
    public ApiResponse<AiChatSessionResponse> createSession(
            @RequestBody @Valid CreateAiChatSessionRequest request,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<AiChatSessionResponse>builder()
                .message("Create AI chat session successfully")
                .result(sessionService.createSession(request, authentication))
                .build();
    }

    /**
     * Lay danh sach chat session cua user dang dang nhap.
     * FE dung endpoint nay de hien sidebar lich su chat.
     */
    @GetMapping("/sessions")
    public ApiResponse<List<AiChatSessionResponse>> getMySessions(JwtAuthenticationToken authentication) {
        return ApiResponse.<List<AiChatSessionResponse>>builder()
                .message("Get AI chat sessions successfully")
                .result(sessionService.getMySessions(authentication))
                .build();
    }

    /**
     * Gui mot cau hoi vao session.
     * Service se kiem tra quyen, retrieval chunks, goi LLM client va luu lich su.
     */
    @PostMapping("/sessions/{sessionId}/messages")
    public ApiResponse<SendAiMessageResponse> sendMessage(
            @PathVariable String sessionId,
            @RequestBody @Valid SendAiMessageRequest request,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<SendAiMessageResponse>builder()
                .message("Send AI message successfully")
                .result(chatService.sendMessage(sessionId, request, authentication))
                .build();
    }

    /**
     * Lay lich su message cua mot chat session theo thu tu thoi gian.
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<AiChatMessageResponse>> getMessages(
            @PathVariable String sessionId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<List<AiChatMessageResponse>>builder()
                .message("Get AI chat messages successfully")
                .result(chatService.getMessages(sessionId, authentication))
                .build();
    }
}
