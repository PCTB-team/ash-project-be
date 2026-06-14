package com.pctb.webapp.ai.client;

import org.springframework.stereotype.Service;

/**
 * AI gia de test tron flow backend ma khong can API key.
 * Khi gan AI that, tao implementation moi cua LlmClient va thay bean nay.
 */
@Service
public class FakeLlmClient implements LlmClient {
    /**
     * Tra ve cau tra loi demo de biet backend da build prompt va retrieval thanh cong.
     */
    @Override
    public String chat(String systemPrompt, String userPrompt) {
        return "AI demo response: backend da nhan cau hoi, da retrieval context va san sang gan model AI that.";
    }
}
