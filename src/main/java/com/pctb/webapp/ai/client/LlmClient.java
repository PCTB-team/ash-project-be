package com.pctb.webapp.ai.client;

/**
 * Contract chung de goi model AI tra loi cau hoi.
 * Ban MVP dung FakeLlmClient, sau nay thay bang OpenAI/Gemini client that.
 */
public interface LlmClient {
    /**
     * Gui system prompt va user prompt sang model AI, nhan ve cau tra loi dang text.
     */
    String chat(String systemPrompt, String userPrompt);
}
