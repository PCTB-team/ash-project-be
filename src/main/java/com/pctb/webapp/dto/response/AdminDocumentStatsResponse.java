package com.pctb.webapp.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminDocumentStatsResponse {
    long totalSystemStorageBytes;   // Tổng dung lượng hệ thống thực tế đang dùng
    String largestFileName;         // Tên tệp tin có kích thước lớn nhất hệ thống
    long largestFileSize;           // Kích thước của tệp lớn nhất đó
    String topUploaderUsername;     // Tên tài khoản upload nhiều tài liệu nhất
    long topUploaderFileCount;      // Số lượng file ông đó đã đăng
}