package com.pctb.webapp.ai.service;

import com.pctb.webapp.ai.entity.DocumentChunk;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Tao prompt dua tren cau hoi va cac chunks retrieval duoc.
 */
@Service
public class PromptBuilderService {
    /**
     * System prompt dat rule cho AI: chi tra loi dua tren context.
     */
    public String systemPrompt() {
        return """
                You are an AI study assistant.
                Answer only using the provided context.
                If the context is not enough, say that the document does not contain enough information.
                """;
    }

    /**
     * Ghep cac chunks thanh context va dat cau hoi cua user o cuoi prompt.
     */
    public String buildUserPrompt(String question, List<DocumentChunk> chunks) {
        String context = chunks.stream()
                .map(chunk -> "[Source " + chunk.getChunkIndex() + "] " + chunk.getContent())
                .collect(Collectors.joining("\n\n"));

        return """
                Context:
                %s

                Question:
                %s
                """.formatted(context, question);
    }
}
