package com.pctb.webapp.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

// Response chuẩn cho luồng chat AI trên tài liệu đã lưu của user.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiDocumentChatResponse {
    // Nội dung câu trả lời cuối cùng của AI.
    String answer;

    // Nguồn câu trả lời: từ tài liệu hay từ kiến thức chung.
    AiAnswerSource answerSource;

    // Danh sách nguồn tham chiếu mà AI đã dùng để trả lời.
    List<AiChatSourceResponse> sources;
}
