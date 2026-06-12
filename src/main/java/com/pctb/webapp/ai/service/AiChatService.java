package com.pctb.webapp.ai.service;

import com.pctb.webapp.ai.client.LlmClient;
import com.pctb.webapp.ai.dto.request.SendAiMessageRequest;
import com.pctb.webapp.ai.dto.response.AiChatMessageResponse;
import com.pctb.webapp.ai.dto.response.AiCitationResponse;
import com.pctb.webapp.ai.dto.response.SendAiMessageResponse;
import com.pctb.webapp.ai.entity.AiChatMessage;
import com.pctb.webapp.ai.entity.AiChatSession;
import com.pctb.webapp.ai.entity.DocumentChunk;
import com.pctb.webapp.ai.entity.MessageRole;
import com.pctb.webapp.ai.repository.AiChatMessageRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Xu ly flow chinh khi user chat voi AI.
 * Flow: verify session -> luu cau hoi -> retrieval chunks -> goi LLM -> luu cau tra loi -> tra response.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AiChatService {
    AiChatSessionService sessionService;

    AiChatMessageRepo messageRepo;

    RetrievalService retrievalService;

    PromptBuilderService promptBuilderService;

    LlmClient llmClient;

    /**
     * Nhan cau hoi cua user, lay context lien quan va tra cau tra loi AI.
     */
    @Transactional
    public SendAiMessageResponse sendMessage(
            String sessionId,
            SendAiMessageRequest request,
            JwtAuthenticationToken authentication
    ) {
        AiChatSession session = sessionService.getOwnedActiveSession(sessionId, authentication.getName());
        String question = request.getMessage().trim();

        // Luu message USER truoc de co lich su day du.
        messageRepo.save(AiChatMessage.builder()
                .session(session)
                .role(MessageRole.USER)
                .content(question)
                .createdAt(LocalDateTime.now())
                .build());

        // Retrieval lay chunks dung scope session, sau do build prompt cho LLM.
        List<DocumentChunk> chunks = retrievalService.retrieve(session, question, 5);
        String answer = llmClient.chat(
                promptBuilderService.systemPrompt(),
                promptBuilderService.buildUserPrompt(question, chunks)
        );

        // Luu message ASSISTANT de endpoint history co the hien lai.
        AiChatMessage assistantMessage = messageRepo.save(AiChatMessage.builder()
                .session(session)
                .role(MessageRole.ASSISTANT)
                .content(answer)
                .modelName("fake-llm")
                .createdAt(LocalDateTime.now())
                .build());

        return SendAiMessageResponse.builder()
                .sessionId(session.getId())
                .messageId(assistantMessage.getId())
                .answer(answer)
                .citations(toCitations(chunks))
                .build();
    }

    /**
     * Lay lich su message cua session sau khi verify session thuoc user.
     */
    public List<AiChatMessageResponse> getMessages(String sessionId, JwtAuthenticationToken authentication) {
        AiChatSession session = sessionService.getOwnedActiveSession(sessionId, authentication.getName());

        return messageRepo.findBySessionIdOrderByCreatedAtAsc(session.getId())
                .stream()
                .map(this::buildMessageResponse)
                .toList();
    }

    /**
     * Map entity message sang DTO tra ve FE.
     */
    private AiChatMessageResponse buildMessageResponse(AiChatMessage message) {
        return AiChatMessageResponse.builder()
                .messageId(message.getId())
                .role(message.getRole().name())
                .content(message.getContent())
                .createdAt(message.getCreatedAt() == null ? null : message.getCreatedAt().toString())
                .build();
    }

    /**
     * Map chunks da retrieval thanh citations de user thay AI dua vao nguon nao.
     */
    private List<AiCitationResponse> toCitations(List<DocumentChunk> chunks) {
        return chunks.stream()
                .map(chunk -> AiCitationResponse.builder()
                        .chunkId(chunk.getId())
                        .sourceType(chunk.getSourceType().name())
                        .documentId(chunk.getDocumentId())
                        .groupFileId(chunk.getGroupFileId())
                        .preview(chunk.getContentPreview())
                        .pageNumber(chunk.getPageNumber())
                        .build())
                .toList();
    }
}
