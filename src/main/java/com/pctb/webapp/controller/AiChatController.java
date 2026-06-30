package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.AiChatRequest;
import com.pctb.webapp.dto.request.AiKnowledgeChatRequest;
import com.pctb.webapp.dto.response.AiChatConversationPageResponse;
import com.pctb.webapp.dto.response.AiChatHistoryPageResponse;
import com.pctb.webapp.dto.response.AiChatMessageListResponse;
import com.pctb.webapp.dto.response.AiDocumentChatResponse;
import com.pctb.webapp.dto.response.AiChatResponse;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.service.AiChatService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AiChatController {
    AiChatService aiChatService;

    @Operation(summary = "Chat with AI assistant")
    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@RequestBody @Valid AiChatRequest request) {
        return ApiResponse.<AiChatResponse>builder()
                .message("Chat with AI successfully")
                .result(AiChatResponse.builder()
                        .answer(aiChatService.chat(request.getMessage()))
                        .build())
                .build();
    }

    // Endpoint chat theo phạm vi tài liệu đã lưu của user.
    @Operation(summary = "Chat with AI using user's stored knowledge scope")
    @PostMapping("/knowledge/chat")
    public ApiResponse<AiDocumentChatResponse> chatWithKnowledge(
            @RequestBody @Valid AiKnowledgeChatRequest request,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<AiDocumentChatResponse>builder()
                .message("Chat with knowledge successfully")
                .result(aiChatService.chatWithKnowledge(request, authentication))
                .build();
    }

    @Operation(summary = "Get AI knowledge chat conversations")
    @GetMapping("/knowledge/conversations")
    public ApiResponse<AiChatConversationPageResponse> getKnowledgeChatConversations(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<AiChatConversationPageResponse>builder()
                .message("Get AI knowledge chat conversations successfully")
                .result(aiChatService.getKnowledgeChatConversations(page, size, authentication))
                .build();
    }

    @Operation(summary = "Get AI knowledge chat conversation messages")
    @GetMapping("/knowledge/conversations/{conversationId}/messages")
    public ApiResponse<AiChatMessageListResponse> getKnowledgeChatMessages(
            @PathVariable String conversationId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<AiChatMessageListResponse>builder()
                .message("Get AI knowledge chat messages successfully")
                .result(aiChatService.getKnowledgeChatMessages(conversationId, authentication))
                .build();
    }

    @Operation(summary = "Get AI knowledge chat history")
    @GetMapping("/knowledge/history")
    public ApiResponse<AiChatHistoryPageResponse> getKnowledgeChatHistory(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "30") int size,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<AiChatHistoryPageResponse>builder()
                .message("Get AI knowledge chat history successfully")
                .result(aiChatService.getKnowledgeChatHistory(page, size, authentication))
                .build();
    }

    @Operation(summary = "Chat with AI assistant using an uploaded file")
    @PostMapping(value = "/chat-with-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<AiChatResponse> chatWithFile(
            @RequestPart("message") String message,
            @Parameter(
                    description = "Document file for AI to read",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    )
            )
            @RequestPart("file") MultipartFile file
    ) {
        return ApiResponse.<AiChatResponse>builder()
                .message("Chat with file successfully")
                .result(AiChatResponse.builder()
                        .answer(aiChatService.chatWithFile(message, file))
                        .build())
                .build();
    }
}
