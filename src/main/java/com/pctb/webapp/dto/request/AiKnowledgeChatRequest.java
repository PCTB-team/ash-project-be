package com.pctb.webapp.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

// Request tổng quát cho AI chat theo một tài liệu, một folder hoặc toàn bộ tài liệu của user.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiKnowledgeChatRequest {
    // Nếu có giá trị, AI chỉ search trong một tài liệu cụ thể.
    String documentId;

    // Nếu có giá trị, AI chỉ search trong một folder cụ thể.
    String folderId;

    // Câu hỏi người dùng gửi cho AI.
    @NotBlank(message = "AI_MESSAGE_REQUIRED")
    @Size(max = 4000, message = "AI_MESSAGE_TOO_LONG")
    String message;

    // Không cho phép request vừa có documentId vừa có folderId để tránh scope mơ hồ.
    @AssertTrue(message = "AI_SCOPE_INVALID")
    public boolean isScopeValid() {
        return !(hasText(documentId) && hasText(folderId));
    }

    // Helper kiểm tra chuỗi có dữ liệu thực hay không.
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
