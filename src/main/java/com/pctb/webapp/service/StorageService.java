package com.pctb.webapp.service;

import com.pctb.webapp.entity.User;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    // Hàm cũ (Giữ nguyên để tránh gãy code các luồng khác nếu có)
    String upload(MultipartFile file, String fileName);

    // =========================================================================
    // 🟢 BỔ SUNG DÒNG NÀY VÀO INTERFACE CỦA BẠN
    // =========================================================================
    String upload(MultipartFile file, String fileName, User user);

    void delete(String storageUrl);

    String rename(String oldStorageUrl, String newFileName);

    Resource loadAsResource(String storageUrl);
}