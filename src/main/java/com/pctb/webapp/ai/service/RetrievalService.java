package com.pctb.webapp.ai.service;

import com.pctb.webapp.ai.entity.AiChatSession;
import com.pctb.webapp.ai.entity.DocumentChunk;
import com.pctb.webapp.ai.repository.DocumentChunkRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Tim cac chunks lien quan nhat voi cau hoi cua user.
 * MVP dang dung keyword score, sau nay co the thay bang vector similarity.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RetrievalService {
    DocumentChunkRepo chunkRepo;

    /**
     * Lay chunks theo scope cua session, cham diem theo keyword va tra topK chunks.
     */
    public List<DocumentChunk> retrieve(AiChatSession session, String question, int topK) {
        List<DocumentChunk> chunks = switch (session.getScopeType()) {
            case PERSONAL_DOCUMENT -> chunkRepo.findByDocumentIdAndDeletedFalseOrderByChunkIndexAsc(session.getDocumentId());
            case PERSONAL_FOLDER -> chunkRepo.findByOwnerIdAndFolderIdAndDeletedFalse(session.getOwner().getId(), session.getFolderId());
            case PERSONAL_LIBRARY -> chunkRepo.findByOwnerIdAndDeletedFalse(session.getOwner().getId());
            case GROUP_DOCUMENTS -> chunkRepo.findByGroupIdAndDeletedFalse(session.getGroupId());
        };

        return chunks.stream()
                .sorted(Comparator.comparingInt(chunk -> -score(chunk.getContent(), question)))
                .limit(Math.max(1, topK))
                .toList();
    }

    /**
     * Cham diem don gian: cau hoi co bao nhieu tu xuat hien trong chunk.
     */
    private int score(String content, String question) {
        if (content == null || question == null) {
            return 0;
        }

        String lowerContent = content.toLowerCase(Locale.ROOT);
        return Arrays.stream(question.toLowerCase(Locale.ROOT).split("\\s+"))
                .filter(word -> word.length() > 1)
                .mapToInt(word -> lowerContent.contains(word) ? 1 : 0)
                .sum();
    }
}
