package com.pctb.webapp.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    // Upload file lên hệ thống lưu trữ và trả về URL/path để lưu vào database.
    String upload(MultipartFile file, String fileName);

    // Xóa file khỏi hệ thống lưu trữ dựa trên URL/path đã lưu.
    void delete(String storageUrl);

    // Đổi tên file trong storage và trả về URL/path mới sau khi đổi tên.
    String rename(String oldStorageUrl, String newFileName);

    // Tải file thành Resource để controller có thể trả nội dung file cho client.
    Resource loadAsResource(String storageUrl);
}
