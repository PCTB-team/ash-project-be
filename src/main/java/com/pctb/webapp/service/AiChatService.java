package com.pctb.webapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;

@Service
public class AiChatService {
    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.5-flash}")
    private String model;

    public AiChatService() {
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .build();
        this.jsonMapper = JsonMapper.builder().build();
    }

    public String chat(String message) {
        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of(
                        "parts", new Object[]{
                                Map.of("text", "Ban la tro ly AI trong he thong quan ly tai lieu. Tra loi ngan gon, ro rang, bang tieng Viet.")
                        }
                ),
                "contents", new Object[]{
                        Map.of(
                                "parts", new Object[]{
                                        Map.of("text", message)
                                }
                        )
                }
        );

        String responseBody = restClient.post()
                .uri("/models/{model}:generateContent", model)
                .header("x-goog-api-key", apiKey)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .body(requestBody)
                .retrieve()
                .body(String.class);

        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }

        JsonNode response = jsonMapper.readTree(responseBody);
        return response.path("candidates")
                .path(0)
                .path("content")
                .path("parts")
                .path(0)
                .path("text")
                .asString();
    }
}
