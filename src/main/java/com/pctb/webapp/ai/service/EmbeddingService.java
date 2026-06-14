package com.pctb.webapp.ai.service;

import java.util.List;

/**
 * Contract tao embedding vector cho text.
 * MVP dang dung NoopEmbeddingService, sau nay gan OpenAI/Gemini embedding vao day.
 */
public interface EmbeddingService {
    /**
     * Bien mot doan text thanh vector so de semantic search.
     */
    List<Double> embed(String text);
}
