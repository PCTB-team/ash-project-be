package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.AiKnowledgeChatRequest;
import com.pctb.webapp.dto.response.AiAnswerSource;
import com.pctb.webapp.dto.response.AiChatHistoryPageResponse;
import com.pctb.webapp.dto.response.AiChatHistoryResponse;
import com.pctb.webapp.dto.response.AiChatSourceResponse;
import com.pctb.webapp.dto.response.AiDocumentChatResponse;
import com.pctb.webapp.entity.AiChatHistory;
import com.pctb.webapp.entity.Document;
import com.pctb.webapp.entity.Folder;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.AiChatHistoryRepo;
import com.pctb.webapp.repository.DocumentRepo;
import com.pctb.webapp.repository.FolderRepo;
import com.pctb.webapp.repository.UserRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.ArrayDeque;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AiChatService {
    private static final String SYSTEM_INSTRUCTION = "Ban la tro ly AI trong he thong quan ly tai lieu. Tra loi ngan gon, ro rang, bang tieng Viet.";
    private static final String KNOWLEDGE_ONLY_NO_CONTEXT_MESSAGE = "Toi khong tim thay thong tin lien quan trong cac tai lieu hien co.";
    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);
    private static final int GEMINI_MAX_RETRIES = 3;
    private static final String AI_BUSY_MESSAGE = "AI hien dang qua tai, vui long thu lai sau.";
    private static final String AI_UNAVAILABLE_MESSAGE = "AI service tam thoi khong kha dung.";

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final DocumentTextExtractorService documentTextExtractorService;
    private final DocumentRepo documentRepo;
    private final FolderRepo folderRepo;
    private final QdrantService qdrantService;
    private final AiChatHistoryRepo aiChatHistoryRepo;
    private final UserRepo userRepo;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.max-output-tokens:1024}")
    private int maxOutputTokens;

    public AiChatService(
            DocumentTextExtractorService documentTextExtractorService,
            DocumentRepo documentRepo,
            FolderRepo folderRepo,
            QdrantService qdrantService,
            AiChatHistoryRepo aiChatHistoryRepo,
            UserRepo userRepo
    ) {
        this.documentTextExtractorService = documentTextExtractorService;
        this.documentRepo = documentRepo;
        this.folderRepo = folderRepo;
        this.qdrantService = qdrantService;
        this.aiChatHistoryRepo = aiChatHistoryRepo;
        this.userRepo = userRepo;
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
    @Transactional
    public AiDocumentChatResponse chatWithKnowledge(
            AiKnowledgeChatRequest request,
            JwtAuthenticationToken authentication
    ) {
        // Lấy user hiện tại để khóa quyền truy cập và retrieval theo owner.
        User currentUser = getCurrentUser(authentication);
        String userId = currentUser.getId();
        Document inferredDocument = resolveDocumentFromMessage(request.getMessage(), userId);
        Folder inferredFolder = resolveFolderFromMessage(request.getMessage(), userId);

        // Xác định phạm vi chat: một tài liệu, một folder hoặc toàn bộ tài liệu.
        AiKnowledgeScope scope = resolveKnowledgeScope(request, inferredDocument, inferredFolder);

        // Kiểm tra quyền truy cập theo scope trước khi search dữ liệu.
        switch (scope) {
            case DOCUMENT -> {
                if (hasText(request.getDocumentId())) {
                    validateDocumentScopeAccess(request.getDocumentId(), userId);
                }
            }
            case FOLDER -> validateFolderScopeAccess(
                    hasText(request.getFolderId()) ? request.getFolderId() : inferredFolder.getId(),
                    userId
            );
            case ALL_DOCUMENTS -> {
            }
        }

        // Chuẩn hóa điều kiện search thành một object filter nội bộ.
        AiKnowledgeFilter filter = buildKnowledgeFilter(request, userId, scope, inferredDocument, inferredFolder);

        // Retrieve cac chunk lien quan tu lop vector search.
        List<AiRetrievedChunk> retrievedChunks = retrieveKnowledgeChunks(
                request.getMessage(),
                filter
        );

        if (hasRelevantContext(retrievedChunks, scope)) {
            String answer = generateDocumentGroundedAnswer(request.getMessage(), retrievedChunks);
            if (isAiTemporaryFailure(answer)) {
                answer = buildFallbackAnswerFromSources(retrievedChunks);
            }

            AiDocumentChatResponse response = AiDocumentChatResponse.builder()
                    .answer(answer)
                    .answerSource(AiAnswerSource.DOCUMENT)
                    .sources(toChatSources(retrievedChunks))
                    .build();

            AiChatHistory history = saveKnowledgeChatHistory(
                    currentUser,
                    resolveHistoryDocumentId(request, inferredDocument),
                    resolveHistoryFolderId(request, inferredFolder),
                    request.getMessage(),
                    response.getAnswer(),
                    response.getAnswerSource()
            );
            response.setHistoryId(history.getId());

            return response;
        }

        AiDocumentChatResponse response = AiDocumentChatResponse.builder()
                .answer(KNOWLEDGE_ONLY_NO_CONTEXT_MESSAGE)
                .answerSource(AiAnswerSource.DOCUMENT)
                .sources(List.of())
                .build();

        AiChatHistory history = saveKnowledgeChatHistory(
                currentUser,
                resolveHistoryDocumentId(request, inferredDocument),
                resolveHistoryFolderId(request, inferredFolder),
                request.getMessage(),
                response.getAnswer(),
                response.getAnswerSource()
        );
        response.setHistoryId(history.getId());

        return response;
    }

    // Suy ra scope chat từ request đầu vào.
    @Transactional(readOnly = true)
    public AiChatHistoryPageResponse getKnowledgeChatHistory(
            int page,
            int size,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(normalizePage(page), normalizePageSize(size));
        Page<AiChatHistory> historyPage = aiChatHistoryRepo.findByUserIdOrderByCreatedAtDesc(currentUser.getId(), pageable);

        return AiChatHistoryPageResponse.builder()
                .items(historyPage.getContent().stream()
                        .map(this::buildChatHistoryResponse)
                        .toList())
                .page(historyPage.getNumber())
                .size(historyPage.getSize())
                .totalElements(historyPage.getTotalElements())
                .totalPages(historyPage.getTotalPages())
                .build();
    }

    private AiChatHistory saveKnowledgeChatHistory(
            User user,
            String documentId,
            String folderId,
            String question,
            String answer,
            AiAnswerSource answerSource
    ) {
        return aiChatHistoryRepo.save(AiChatHistory.builder()
                .user(user)
                .documentId(documentId)
                .folderId(folderId)
                .question(question)
                .answer(answer)
                .answerSource(answerSource == null ? null : answerSource.name())
                .createdAt(LocalDateTime.now())
                .build());
    }

    private AiChatHistoryResponse buildChatHistoryResponse(AiChatHistory history) {
        return AiChatHistoryResponse.builder()
                .historyId(history.getId())
                .documentId(history.getDocumentId())
                .folderId(history.getFolderId())
                .question(history.getQuestion())
                .answer(history.getAnswer())
                .answerSource(history.getAnswerSource())
                .createdAt(history.getCreatedAt() == null ? null : history.getCreatedAt().toString())
                .build();
    }

    private User getCurrentUser(JwtAuthenticationToken authentication) {
        return userRepo.findById(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private String resolveHistoryDocumentId(
            AiKnowledgeChatRequest request,
            Document inferredDocument
    ) {
        if (hasText(request.getDocumentId())) {
            return request.getDocumentId();
        }

        return inferredDocument == null ? null : inferredDocument.getId();
    }

    private String resolveHistoryFolderId(
            AiKnowledgeChatRequest request,
            Folder inferredFolder
    ) {
        if (hasText(request.getFolderId())) {
            return request.getFolderId();
        }

        return inferredFolder == null ? null : inferredFolder.getId();
    }

    private AiKnowledgeScope resolveKnowledgeScope(
            AiKnowledgeChatRequest request,
            Document inferredDocument,
            Folder inferredFolder
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

        if (inferredFolder != null) {
            return AiKnowledgeScope.FOLDER;
        }

        return AiKnowledgeScope.ALL_DOCUMENTS;
    }

    // Chuyển request và scope thành filter chuẩn cho lớp retrieval.
    private AiKnowledgeFilter buildKnowledgeFilter(
            AiKnowledgeChatRequest request,
            String userId,
            AiKnowledgeScope scope,
            Document inferredDocument,
            Folder inferredFolder
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
                    .folderId(hasText(request.getFolderId())
                            ? request.getFolderId()
                            : inferredFolder.getId())
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
        if (filter.getFolderId() != null && !filter.getFolderId().isBlank()) {
            return retrieveKnowledgeChunksForFolderScope(query, filter);
        }

        return qdrantService.searchKnowledgeChunks(query, filter);
    }

    private List<AiRetrievedChunk> retrieveKnowledgeChunksForFolderScope(
            String query,
            AiKnowledgeFilter filter
    ) {
        Set<String> folderIds = collectFolderTreeIds(filter.getOwnerId(), filter.getFolderId());
        if (folderIds.isEmpty()) {
            return List.of();
        }

        List<Document> documents = documentRepo.findActiveByOwnerId(filter.getOwnerId()).stream()
                .filter(document -> document.getFolder() != null)
                .filter(document -> folderIds.contains(document.getFolder().getId()))
                .toList();

        if (documents.isEmpty()) {
            return List.of();
        }

        Map<String, AiRetrievedChunk> bestChunkByDocumentAndIndex = new LinkedHashMap<>();

        for (Document document : documents) {
            AiKnowledgeFilter documentFilter = AiKnowledgeFilter.builder()
                    .ownerId(filter.getOwnerId())
                    .documentId(document.getId())
                    .build();

            for (AiRetrievedChunk chunk : qdrantService.searchKnowledgeChunks(query, documentFilter)) {
                String chunkKey = chunk.getDocumentId() + ":" + chunk.getChunkIndex();
                AiRetrievedChunk existingChunk = bestChunkByDocumentAndIndex.get(chunkKey);

                if (existingChunk == null
                        || (chunk.getScore() != null
                        && (existingChunk.getScore() == null || chunk.getScore() > existingChunk.getScore()))) {
                    bestChunkByDocumentAndIndex.put(chunkKey, chunk);
                }
            }
        }

        return bestChunkByDocumentAndIndex.values().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(
                        AiRetrievedChunk::getScore,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .limit(5)
                .toList();
    }

    private Set<String> collectFolderTreeIds(String ownerId, String rootFolderId) {
        Folder rootFolder = folderRepo.findActiveByIdAndOwnerId(rootFolderId, ownerId)
                .orElse(null);
        if (rootFolder == null) {
            return Set.of();
        }

        Map<String, List<Folder>> childFoldersByParentId = folderRepo.findActiveByOwnerId(ownerId).stream()
                .filter(folder -> folder.getParent() != null)
                .collect(Collectors.groupingBy(folder -> folder.getParent().getId()));

        Set<String> folderIds = new java.util.LinkedHashSet<>();
        ArrayDeque<String> pendingFolderIds = new ArrayDeque<>();
        pendingFolderIds.add(rootFolder.getId());

        while (!pendingFolderIds.isEmpty()) {
            String currentFolderId = pendingFolderIds.removeFirst();
            if (!folderIds.add(currentFolderId)) {
                continue;
            }

            for (Folder childFolder : childFoldersByParentId.getOrDefault(currentFolderId, List.of())) {
                pendingFolderIds.addLast(childFolder.getId());
            }
        }

        return folderIds;
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
                Khong duoc tra loi bang kien thuc ben ngoai tai lieu.
                """.formatted(userMessage, context);
    }

    private String generateDocumentGroundedAnswer(
            String userMessage,
            List<AiRetrievedChunk> retrievedChunks
    ) {
        String prompt = buildDocumentGroundedPrompt(userMessage, retrievedChunks);
        return generate(prompt);
    }

    private boolean isAiTemporaryFailure(String answer) {
        return AI_BUSY_MESSAGE.equals(answer) || AI_UNAVAILABLE_MESSAGE.equals(answer);
    }

    private String buildFallbackAnswerFromSources(List<AiRetrievedChunk> retrievedChunks) {
        String extractedContent = retrievedChunks.stream()
                .limit(2)
                .map(AiRetrievedChunk::getExcerpt)
                .filter(this::hasText)
                .map(excerpt -> excerpt.length() > 500 ? excerpt.substring(0, 500) + "..." : excerpt)
                .collect(Collectors.joining("\n\n"));

        if (!hasText(extractedContent)) {
            return "Da tim thay tai lieu lien quan, nhung AI tam thoi khong kha dung de tong hop cau tra loi.";
        }

        return "Da tim thay tai lieu lien quan, nhung AI tam thoi khong kha dung. Noi dung trich xuat:\n\n"
                + extractedContent;
    }

    private boolean hasRelevantContext(
            List<AiRetrievedChunk> retrievedChunks,
            AiKnowledgeScope scope
    ) {
        if (retrievedChunks == null || retrievedChunks.isEmpty()) {
            return false;
        }

        if (scope == AiKnowledgeScope.DOCUMENT || scope == AiKnowledgeScope.FOLDER) {
            return true;
        }

        return retrievedChunks.stream()
                .anyMatch(chunk -> chunk.getScore() != null && chunk.getScore() >= 0.7);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizePageSize(int size) {
        if (size < 1) {
            return 30;
        }

        return Math.min(size, 100);
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

    private Folder resolveFolderFromMessage(String message, String userId) {
        if (!hasText(message)) {
            return null;
        }

        String normalizedMessage = normalizeTextForMatching(message);

        return folderRepo.findActiveByOwnerId(userId).stream()
                .filter(folder -> matchesFolderName(normalizedMessage, folder.getName()))
                .max(Comparator.comparingInt(folder ->
                        folder.getName() == null ? 0 : folder.getName().length()))
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

    private boolean matchesFolderName(String normalizedMessage, String folderName) {
        if (!hasText(folderName)) {
            return false;
        }

        String normalizedFolderName = normalizeTextForMatching(folderName);
        return normalizedFolderName.length() >= 3 && normalizedMessage.contains(normalizedFolderName);
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
        return executeGeminiRequestWithRetry(requestBody, model);
    }

    private String executeGeminiRequestWithRetry(
            Map<String, Object> requestBody,
            String targetModel
    ) {
        for (int attempt = 1; attempt <= GEMINI_MAX_RETRIES; attempt++) {
            try {
                return executeGeminiRequestOnce(requestBody, targetModel);
            } catch (HttpServerErrorException.ServiceUnavailable exception) {
                log.warn("Gemini returned 503 on attempt {}/{} for model {}", attempt, GEMINI_MAX_RETRIES, targetModel);
                if (attempt == GEMINI_MAX_RETRIES) {
                    return AI_BUSY_MESSAGE;
                }
                sleepBeforeRetry(attempt);
            } catch (HttpServerErrorException exception) {
                log.error("Gemini server error {} for model {}", exception.getStatusCode(), targetModel, exception);
                return AI_UNAVAILABLE_MESSAGE;
            } catch (ResourceAccessException exception) {
                log.error("Gemini request failed due to network or timeout issue for model {}", targetModel, exception);
                return AI_UNAVAILABLE_MESSAGE;
            }
        }

        return AI_BUSY_MESSAGE;
    }

    private String executeGeminiRequestOnce(
            Map<String, Object> requestBody,
            String targetModel
    ) {
        String responseBody = restClient.post()
                .uri("/models/{model}:generateContent", targetModel)
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

    private void sleepBeforeRetry(int attempt) {
        try {
            long exponentialDelay = 1000L * (1L << (attempt - 1));
            long jitter = java.util.concurrent.ThreadLocalRandom.current().nextLong(200L, 800L);
            Thread.sleep(exponentialDelay + jitter);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Gemini retry interrupted", exception);
        }
    }
}
