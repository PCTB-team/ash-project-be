package com.pctb.webapp.ai.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Cat raw text thanh nhieu doan nho de AI retrieval de hon.
 */
@Service
public class ChunkingService {
    private static final int CHUNK_SIZE = 3000;
    private static final int OVERLAP = 400;

    /**
     * Normalize text, cat thanh chunks co overlap de khong mat ngu canh giua 2 doan.
     */
    public List<String> chunk(String text) {
        String normalized = normalize(text);
        if (normalized.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;

        while (start < normalized.length()) {
            int end = Math.min(start + CHUNK_SIZE, normalized.length());
            chunks.add(normalized.substring(start, end));

            if (end == normalized.length()) {
                break;
            }

            start = Math.max(0, end - OVERLAP);
        }

        return chunks;
    }

    /**
     * Don sach khoang trang va xuong dong bi rac sau khi parse PDF/DOCX/TXT.
     */
    private String normalize(String text) {
        if (text == null) {
            return "";
        }

        return text.replaceAll("\\s+", " ").trim();
    }
}
