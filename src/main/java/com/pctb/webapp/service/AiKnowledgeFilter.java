package com.pctb.webapp.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

// Bộ lọc nội bộ dùng để chuẩn hóa điều kiện search cho lớp retrieval.
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiKnowledgeFilter {
    // Chủ sở hữu dữ liệu, luôn phải có để khóa retrieval theo user hiện tại.
    String ownerId;

    // Tài liệu cụ thể cần search nếu scope là DOCUMENT.
    String documentId;

    // Folder cụ thể cần search nếu scope là FOLDER.
    String folderId;
}
