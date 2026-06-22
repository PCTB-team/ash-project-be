package com.pctb.webapp.service;

import com.pctb.webapp.entity.Document;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.DocumentRepo;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DocumentIndexingService {
    private final DocumentRepo documentRepo;
    private final StorageService storageService;
    private final DocumentTextExtractorService documentTextExtractorService;
    private final EmbeddingService embeddingService;
    private final QdrantService qdrantService;

    public DocumentIndexingService(
            DocumentRepo documentRepo,
            StorageService storageService,
            DocumentTextExtractorService documentTextExtractorService,
            EmbeddingService embeddingService,
            QdrantService qdrantService
    ) {
        this.documentRepo = documentRepo;
        this.storageService = storageService;
        this.documentTextExtractorService = documentTextExtractorService;
        this.embeddingService = embeddingService;
        this.qdrantService = qdrantService;
    }

    public void indexDocument(String documentId) {
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (Boolean.TRUE.equals(document.getDeleted())) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        Resource resource = storageService.loadAsResource(document.getStorageUrl());
        String extractedText = documentTextExtractorService.extractForIndexing(
                resource,
                document.getFileName()
        );
        List<String> chunks = splitIntoChunks(extractedText);
        List<List<Double>> chunkVectors = chunks.stream()
                .map(chunk -> embeddingService.embedText(chunk))
                .toList();
        qdrantService.upsertDocumentChunks(document, chunks, chunkVectors);
    }

    private List<String> splitIntoChunks(String text) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        String normalizedText = text.replaceAll("\\s+", " ").trim();
        int chunkSize = 1000;
        int overlap = 200;
        int start = 0;

        while (start < normalizedText.length()) {
            int end = Math.min(start + chunkSize, normalizedText.length());
            chunks.add(normalizedText.substring(start, end));

            if (end == normalizedText.length()) {
                break;
            }

            start = Math.max(end - overlap, start + 1);
        }

        return chunks;
    }
}
