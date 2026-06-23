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

        String normalizedText = text.replace("\r", "").trim();
        String[] paragraphs = normalizedText.split("\\n\\s*\\n+");
        int maxChunkLength = 1200;
        int overlapLength = 200;

        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            String cleanedParagraph = paragraph.replaceAll("\\s+", " ").trim();
            if (cleanedParagraph.isBlank()) {
                continue;
            }

            if (currentChunk.length() == 0) {
                appendParagraph(currentChunk, cleanedParagraph);
                continue;
            }

            if (currentChunk.length() + 2 + cleanedParagraph.length() <= maxChunkLength) {
                appendParagraph(currentChunk, cleanedParagraph);
                continue;
            }

            chunks.add(currentChunk.toString());

            String overlap = currentChunk.length() <= overlapLength
                    ? currentChunk.toString()
                    : currentChunk.substring(currentChunk.length() - overlapLength);

            currentChunk = new StringBuilder(overlap);
            appendParagraph(currentChunk, cleanedParagraph);
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }

    private void appendParagraph(StringBuilder chunkBuilder, String paragraph) {
        if (chunkBuilder.length() > 0) {
            chunkBuilder.append("\n\n");
        }
        chunkBuilder.append(paragraph);
    }
}
