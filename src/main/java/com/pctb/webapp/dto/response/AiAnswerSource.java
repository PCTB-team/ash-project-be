package com.pctb.webapp.dto.response;

// Phân loại nguồn gốc câu trả lời AI để frontend hiển thị đúng ngữ cảnh.
public enum AiAnswerSource {
    // Câu trả lời được grounding từ tài liệu của user.
    DOCUMENT,

    // Câu trả lời dùng kiến thức chung khi tài liệu không đủ context.
    GENERAL
}
