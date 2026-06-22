package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.AiKnowledgeChatRequest;
import com.pctb.webapp.dto.response.AiAnswerSource;
import com.pctb.webapp.dto.response.AiChatSourceResponse;
import com.pctb.webapp.dto.response.AiDocumentChatResponse;
import com.pctb.webapp.entity.Document;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.DocumentRepo;
import com.pctb.webapp.repository.FolderRepo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiChatService {
    private static final String SYSTEM_INSTRUCTION = "Ban la tro ly AI trong he thong quan ly tai lieu. Tra loi ngan gon, ro rang, bang tieng Viet.";

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final DocumentTextExtractorService documentTextExtractorService;
    private final DocumentRepo documentRepo;
    private final FolderRepo folderRepo;
    private final QdrantService qdrantService;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.5-flash}")
    private String model;

    @Value("${gemini.max-output-tokens:4096}")
    private int maxOutputTokens;

    public AiChatService(
            DocumentTextExtractorService documentTextExtractorService,
            DocumentRepo documentRepo,
            FolderRepo folderRepo,
            QdrantService qdrantService
    ) {
        this.documentTextExtractorService = documentTextExtractorService;
        this.documentRepo = documentRepo;
        this.folderRepo = folderRepo;
        this.qdrantService = qdrantService;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(15));
        requestFactory.setReadTimeout(Duration.ofSeconds(120));

        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .requestFactory(requestFactory)
                .build();
        this.jsonMapper = JsonMapper.builder().build();
    }

    public String chat(String message) {
        return generate(message);
    }

    // Luồng chat AI theo tài liệu đã lưu của user.
    // Hiện tại mới dựng khung: resolve scope, check quyền, build filter.
    public AiDocumentChatResponse chatWithKnowledge(
            AiKnowledgeChatRequest request,
            JwtAuthenticationToken authentication
    ) {
        // Lấy user hiện tại để khóa quyền truy cập và retrieval theo owner.
        String userId = authentication.getName();
        Document inferredDocument = resolveDocumentFromMessage(request.getMessage(), userId);

        // Xác định phạm vi chat: một tài liệu, một folder hoặc toàn bộ tài liệu.
        AiKnowledgeScope scope = resolveKnowledgeScope(request, inferredDocument);

        // Kiểm tra quyền truy cập theo scope trước khi search dữ liệu.
        switch (scope) {
            case DOCUMENT -> {
                if (hasText(request.getDocumentId())) {
                    validateDocumentScopeAccess(request.getDocumentId(), userId);
                }
            }
            case FOLDER -> validateFolderScopeAccess(request.getFolderId(), userId);
            case ALL_DOCUMENTS -> {
            }
        }

        // Chuẩn hóa điều kiện search thành một object filter nội bộ.
        AiKnowledgeFilter filter = buildKnowledgeFilter(request, userId, scope, inferredDocument);

        // Retrieve cac chunk lien quan tu lop vector search.
        List<AiRetrievedChunk> retrievedChunks = retrieveKnowledgeChunks(
                request.getMessage(),
                filter
        );

        if (hasRelevantContext(retrievedChunks, scope)) {
            return AiDocumentChatResponse.builder()
                    .answer(generateDocumentGroundedAnswer(request.getMessage(), retrievedChunks))
                    .answerSource(AiAnswerSource.DOCUMENT)
                    .sources(toChatSources(retrievedChunks))
                    .build();
        }

        return AiDocumentChatResponse.builder()
                .answer(generateGeneralAnswer(request.getMessage()))
                .answerSource(AiAnswerSource.GENERAL)
                .sources(List.of())
                .build();
    }

    // Suy ra scope chat từ request đầu vào.
    private AiKnowledgeScope resolveKnowledgeScope(
            AiKnowledgeChatRequest request,
            Document inferredDocument
    ) {
        if (hasText(request.getDocumentId())) {
            return AiKnowledgeScope.DOCUMENT;
        }

        if (hasText(request.getFolderId())) {
            return AiKnowledgeScope.FOLDER;
        }

        if (inferredDocument != null) {
            return AiKnowledgeScope.DOCUMENT;
        }

        return AiKnowledgeScope.ALL_DOCUMENTS;
    }

    // Chuyển request và scope thành filter chuẩn cho lớp retrieval.
    private AiKnowledgeFilter buildKnowledgeFilter(
            AiKnowledgeChatRequest request,
            String userId,
            AiKnowledgeScope scope,
            Document inferredDocument
    ) {
        return switch (scope) {
            case DOCUMENT -> AiKnowledgeFilter.builder()
                    .ownerId(userId)
                    .documentId(hasText(request.getDocumentId())
                            ? request.getDocumentId()
                            : inferredDocument.getId())
                    .build();

            case FOLDER -> AiKnowledgeFilter.builder()
                    .ownerId(userId)
                    .folderId(request.getFolderId())
                    .build();

            case ALL_DOCUMENTS -> AiKnowledgeFilter.builder()
                    .ownerId(userId)
                    .build();
        };
    }

    // Retrieve các chunk liên quan theo câu hỏi và phạm vi dữ liệu đã chuẩn hóa.
    private List<AiRetrievedChunk> retrieveKnowledgeChunks(
            String query,
            AiKnowledgeFilter filter
    ) {
        return qdrantService.searchKnowledgeChunks(query, filter);
    }


    private List<AiChatSourceResponse> toChatSources(List<AiRetrievedChunk> retrievedChunks) {
        if (retrievedChunks == null || retrievedChunks.isEmpty()) {
            return List.of();
        }

        return retrievedChunks.stream()
                .map(chunk -> AiChatSourceResponse.builder()
                        .documentId(chunk.getDocumentId())
                        .fileName(chunk.getFileName())
                        .chunkIndex(chunk.getChunkIndex())
                        .excerpt(chunk.getExcerpt())
                        .score(chunk.getScore())
                        .build())
                .toList();
    }

    private String buildDocumentGroundedPrompt(
            String userMessage,
            List<AiRetrievedChunk> retrievedChunks
    ) {
        String context = retrievedChunks.stream()
                .limit(3)
                .map(chunk -> """
                        [Tai lieu: %s | Chunk: %s]
                        %s
                        """.formatted(
                        chunk.getFileName(),
                        chunk.getChunkIndex(),
                        chunk.getContent()
                ))
                .collect(Collectors.joining("\n\n"));

        return """
                Cau hoi cua nguoi dung:
                %s

                Ngu canh tai lieu:
                %s

                Yeu cau:
                Chi tra loi dua tren ngu canh tai lieu da cung cap.
                Neu ngu canh khong du de tra loi day du, hay noi ro phan nao khong co trong tai lieu.
                """.formatted(userMessage, context);
    }

    private String generateDocumentGroundedAnswer(
            String userMessage,
            List<AiRetrievedChunk> retrievedChunks
    ) {
        String prompt = buildDocumentGroundedPrompt(userMessage, retrievedChunks);
        return generate(prompt);
    }

    private String generateGeneralAnswer(String userMessage) {
        return generate(userMessage);
    }

    private boolean hasRelevantContext(
            List<AiRetrievedChunk> retrievedChunks,
            AiKnowledgeScope scope
    ) {
        if (retrievedChunks == null || retrievedChunks.isEmpty()) {
            return false;
        }

        if (scope == AiKnowledgeScope.DOCUMENT) {
            return true;
        }

        return retrievedChunks.stream()
                .anyMatch(chunk -> chunk.getScore() != null && chunk.getScore() >= 0.7);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private Document resolveDocumentFromMessage(String message, String userId) {
        if (!hasText(message)) {
            return null;
        }

        String normalizedMessage = normalizeTextForMatching(message);

        return documentRepo.findActiveByOwnerId(userId).stream()
                .filter(document -> matchesDocumentName(normalizedMessage, document.getFileName()))
                .max(Comparator.comparingInt(document ->
                        document.getFileName() == null ? 0 : document.getFileName().length()))
                .orElse(null);
    }

    private boolean matchesDocumentName(String normalizedMessage, String fileName) {
        if (!hasText(fileName)) {
            return false;
        }

        String normalizedFileName = normalizeTextForMatching(fileName);
        if (normalizedMessage.contains(normalizedFileName)) {
            return true;
        }

        String baseName = removeFileExtension(normalizedFileName);
        return baseName.length() >= 3 && normalizedMessage.contains(baseName);
    }

    private String normalizeTextForMatching(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String removeFileExtension(String fileName) {
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex <= 0) {
            return fileName;
        }

        return fileName.substring(0, extensionIndex);
    }

    // Xác minh document scope thật sự thuộc user hiện tại và chưa bị xóa.
    private void validateDocumentScopeAccess(String documentId, String userId) {
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (!document.getOwner().getId().equals(userId)) {
            throw new AppException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        if (Boolean.TRUE.equals(document.getDeleted())) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
        }
    }

    // Xác minh folder scope là folder active thuộc user hiện tại.
    private void validateFolderScopeAccess(String folderId, String userId) {
        folderRepo.findActiveByIdAndOwnerId(folderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FOLDER_NOT_FOUND));
    }

    public String chatWithFile(String message, MultipartFile file) {
        String extension = documentTextExtractorService.validateSupportedFileAndGetExtension(file);
        if ("pdf".equals(extension)) {
            return generateWithInlinePdf(message, file);
        }

        String fileText = documentTextExtractorService.extractForAi(file);
        String prompt = """
                Noi dung file:
                %s

                Cau hoi cua nguoi dung:
                %s

                Yeu cau:
                Chi tra loi dua tren noi dung file. Neu file khong co thong tin lien quan, hay noi: Toi khong tim thay thong tin trong file.
                """.formatted(fileText, message);

        return generate(prompt);
    }

    private String generateWithInlinePdf(String message, MultipartFile file) {
        String encodedPdf;
        try {
            encodedPdf = Base64.getEncoder().encodeToString(file.getBytes());
        } catch (IOException exception) {
            throw new com.pctb.webapp.exception.AppException(com.pctb.webapp.exception.ErrorCode.REQUEST_PARAMETER_INVALID);
        }

        String prompt = """
                Cau hoi cua nguoi dung:
                %s

                Yeu cau:
                Doc file PDF dinh kem va chi tra loi dua tren noi dung file. Neu file khong co thong tin lien quan, hay noi: Toi khong tim thay thong tin trong file.
                """.formatted(message);

        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of(
                        "parts", new Object[]{
                                Map.of("text", SYSTEM_INSTRUCTION)
                        }
                ),
                "contents", new Object[]{
                        Map.of(
                                "parts", new Object[]{
                                        Map.of("inline_data", Map.of(
                                                "mime_type", "application/pdf",
                                                "data", encodedPdf
                                        )),
                                        Map.of("text", prompt)
                                }
                        )
                },
                "generationConfig", Map.of(
                        "maxOutputTokens", maxOutputTokens,
                        "temperature", 0.3
                )
        );

        return executeGeminiRequest(requestBody);
    }

    private String generate(String message) {
        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of(
                        "parts", new Object[]{
                                Map.of("text", SYSTEM_INSTRUCTION)
                        }
                ),
                "contents", new Object[]{
                        Map.of(
                                "parts", new Object[]{
                                        Map.of("text", message)
                                }
                        )
                },
                "generationConfig", Map.of(
                        "maxOutputTokens", maxOutputTokens,
                        "temperature", 0.3
                )
        );

        return executeGeminiRequest(requestBody);
    }

    private String executeGeminiRequest(Map<String, Object> requestBody) {
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
