package com.pctb.webapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class EmbeddingService {
    private final RestClient restClient;
    private final JsonMapper jsonMapper;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.embedding-model}")
    private String embeddingModel;

    public EmbeddingService() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .requestFactory(requestFactory)
                .build();

        this.jsonMapper = JsonMapper.builder().build();
    }

    public List<Double> embedQuery(String query) {
        return embedText(query);
    }

    public List<Double> embedText(String text) {
        Map<String, Object> requestBody = Map.of(
                "model", "models/" + embeddingModel,
                "content", Map.of(
                        "parts", List.of(
                                Map.of("text", text)
                        )
                )
        );

        String responseBody = restClient.post()
                .uri("/models/{model}:embedContent", embeddingModel)
                .header("x-goog-api-key", apiKey)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        if (responseBody == null || responseBody.isBlank()) {
            return List.of();
        }

        JsonNode values = jsonMapper.readTree(responseBody)
                .path("embedding")
                .path("values");

        List<Double> vector = new java.util.ArrayList<>();
        for (JsonNode value : values) {
            vector.add(value.asDouble());
        }

        return vector;
    }
}
