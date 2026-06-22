package com.pctb.webapp.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

// Mô tả một nguồn tài liệu mà AI đã dùng để trả lời.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiChatSourceResponse {
    // Tài liệu chứa chunk được dùng làm nguồn.
    String documentId;

    // Tên file để frontend hiển thị dễ hiểu hơn documentId.
    String fileName;

    // Chỉ số chunk trong tài liệu đã được retrieve.
    Integer chunkIndex;

    // Đoạn trích ngắn từ chunk để user xem AI đang bám vào đâu.
    String excerpt;

    // Điểm tương đồng từ vector search, hữu ích cho debug và tuning.
    Double score;
}
