package com.pctb.webapp.ai.service;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Embedding gia de MVP chay duoc ma khong can API key.
 * Retrieval hien tai dang keyword-based nen chua can vector that.
 */
@Service
public class NoopEmbeddingService implements EmbeddingService {
    /**
     * Tra vector rong, chi giu cho pipeline ingest co cho cam embedding sau nay.
     */
    @Override
    public List<Double> embed(String text) {
        return List.of();
    }
}
