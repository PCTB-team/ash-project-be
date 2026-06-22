package com.pctb.webapp.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

// Đại diện cho một chunk được retrieve từ lớp vector search.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiRetrievedChunk {
    // Tài liệu chứa chunk được retrieve.
    String documentId;

    // Tên file để phục vụ map sang response cho frontend.
    String fileName;

    // Chỉ số chunk trong tài liệu gốc.
    Integer chunkIndex;

    // Nội dung đầy đủ của chunk để đưa vào prompt cho model.
    String content;

    // Đoạn rút gọn của chunk để hiển thị ở sources.
    String excerpt;

    // Điểm tương đồng trả về từ vector search.
    Double score;
}
