package com.pctb.webapp.service;

import com.pctb.webapp.entity.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class QdrantService {
    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final EmbeddingService embeddingService;

    @Value("${qdrant.api-key:}")
    private String apiKey;

    @Value("${qdrant.collection.document-chunks:document_chunks}")
    private String collectionName;

    public QdrantService(
            @Value("${qdrant.url}") String qdrantUrl,
            EmbeddingService embeddingService
    ) {
        this.embeddingService = embeddingService;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        this.restClient = RestClient.builder()
                .baseUrl(qdrantUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .requestFactory(requestFactory)
                .build();

        this.jsonMapper = JsonMapper.builder().build();
    }

    public List<AiRetrievedChunk> searchKnowledgeChunks(
            String query,
            AiKnowledgeFilter filter
    ) {
        List<Double> queryVector = embeddingService.embedQuery(query);
        Map<String, Object> requestBody = buildSearchRequestBody(queryVector, filter);
        RestClient.RequestBodySpec requestSpec = restClient.post()
                .uri("/collections/{collectionName}/points/search", collectionName);

        String responseBody = applyApiKeyIfPresent(requestSpec)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        if (responseBody == null || responseBody.isBlank()) {
            return List.of();
        }

        JsonNode resultNodes = jsonMapper.readTree(responseBody).path("result");
        List<AiRetrievedChunk> chunks = new java.util.ArrayList<>();

        for (JsonNode resultNode : resultNodes) {
            JsonNode payload = resultNode.path("payload");

            chunks.add(AiRetrievedChunk.builder()
                    .documentId(payload.path("documentId").asText(null))
                    .fileName(payload.path("fileName").asText(null))
                    .chunkIndex(payload.path("chunkIndex").isMissingNode() ? null : payload.path("chunkIndex").asInt())
                    .content(payload.path("content").asText(""))
                    .excerpt(payload.path("content").asText(""))
                    .score(resultNode.path("score").isMissingNode() ? null : resultNode.path("score").asDouble())
                    .build());
        }

        return chunks;
    }

    public void upsertDocumentChunks(
            Document document,
            List<String> chunks,
            List<List<Double>> chunkVectors
    ) {
        Map<String, Object> requestBody = buildUpsertRequestBody(document, chunks, chunkVectors);

        RestClient.RequestBodySpec requestSpec = restClient.put()
                .uri("/collections/{collectionName}/points", collectionName);

        applyApiKeyIfPresent(requestSpec)
                .body(requestBody)
                .retrieve()
                .toBodilessEntity();
    }

    private Map<String, Object> buildChunkPayload(
            Document document,
            String chunkContent,
            int chunkIndex
    ) {
        return Map.of(
                "ownerId", document.getOwner().getId(),
                "documentId", document.getId(),
                "folderId", document.getFolder() == null ? "" : document.getFolder().getId(),
                "fileName", document.getFileName(),
                "chunkIndex", chunkIndex,
                "content", chunkContent
        );
    }

    private Map<String, Object> buildPoint(
            Document document,
            String chunkContent,
            int chunkIndex,
            List<Double> vector
    ) {
        return Map.of(
                "id", buildPointId(document.getId(), chunkIndex),
                "vector", vector,
                "payload", buildChunkPayload(document, chunkContent, chunkIndex)
        );
    }

    private String buildPointId(String documentId, int chunkIndex) {
        String rawId = documentId + ":" + chunkIndex;
        return UUID.nameUUIDFromBytes(rawId.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private List<Map<String, Object>> buildPoints(
            Document document,
            List<String> chunks,
            List<List<Double>> chunkVectors
    ) {
        List<Map<String, Object>> points = new java.util.ArrayList<>();

        int total = Math.min(chunks.size(), chunkVectors.size());
        for (int index = 0; index < total; index++) {
            points.add(buildPoint(
                    document,
                    chunks.get(index),
                    index,
                    chunkVectors.get(index)
            ));
        }

        return points;
    }

    private Map<String, Object> buildUpsertRequestBody(
            Document document,
            List<String> chunks,
            List<List<Double>> chunkVectors
    ) {
        return Map.of(
                "points", buildPoints(document, chunks, chunkVectors)
        );
    }

    private Map<String, Object> buildQdrantFilter(AiKnowledgeFilter filter) {
        List<Map<String, Object>> mustConditions = new java.util.ArrayList<>();

        mustConditions.add(Map.of(
                "key", "ownerId",
                "match", Map.of("value", filter.getOwnerId())
        ));

        if (filter.getDocumentId() != null && !filter.getDocumentId().isBlank()) {
            mustConditions.add(Map.of(
                    "key", "documentId",
                    "match", Map.of("value", filter.getDocumentId())
            ));
        }

        if (filter.getFolderId() != null && !filter.getFolderId().isBlank()) {
            mustConditions.add(Map.of(
                    "key", "folderId",
                    "match", Map.of("value", filter.getFolderId())
            ));
        }

        return Map.of("must", mustConditions);
    }

    private Map<String, Object> buildSearchRequestBody(
            List<Double> queryVector,
            AiKnowledgeFilter filter
    ) {
        return Map.of(
                "vector", queryVector,
                "limit", 5,
                "with_payload", true,
                "filter", buildQdrantFilter(filter)
        );
    }

    private RestClient.RequestBodySpec applyApiKeyIfPresent(RestClient.RequestBodySpec requestSpec) {
        if (apiKey == null || apiKey.isBlank()) {
            return requestSpec;
        }

        return requestSpec.header("api-key", apiKey);
    }
}
