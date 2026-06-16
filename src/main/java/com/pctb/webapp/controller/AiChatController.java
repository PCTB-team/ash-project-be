package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.AiChatRequest;
import com.pctb.webapp.dto.response.AiChatResponse;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
