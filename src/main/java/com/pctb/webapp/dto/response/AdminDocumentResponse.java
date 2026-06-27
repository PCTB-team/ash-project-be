package com.pctb.webapp.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminDocumentResponse {
    String id;
    String fileName;
    String fileExtension;
    Long fileSize;
    String ownerUsername; // Tên tài khoản sinh viên sở hữu tài liệu
    String ownerEmail;    // Email của sinh viên
    boolean deleted;       // Trạng thái đã xóa mềm hay chưa (nằm trong thùng rác)
    LocalDateTime createdAt;
}