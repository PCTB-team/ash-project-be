package com.pctb.webapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Service
public class AiChatService {
    private static final String SYSTEM_INSTRUCTION = "Ban la tro ly AI trong he thong quan ly tai lieu. Tra loi ngan gon, ro rang, bang tieng Viet.";

    private final RestClient restClient;
    private final JsonMapper jsonMapper;
    private final DocumentTextExtractorService documentTextExtractorService;

    @Value("${gemini.api-key}")
    private String apiKey;

    @Value("${gemini.model:gemini-3.5-flash}")
    private String model;

    @Value("${gemini.max-output-tokens:4096}")
    private int maxOutputTokens;

    public AiChatService(DocumentTextExtractorService documentTextExtractorService) {
        this.documentTextExtractorService = documentTextExtractorService;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(15));
        requestFactory.setReadTimeout(Duration.ofSeconds(60));

        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com/v1beta")
                .requestFactory(requestFactory)
                .build();
        this.jsonMapper = JsonMapper.builder().build();
    }

    public String chat(String message) {
        return generate(message);
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
