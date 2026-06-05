package com.pctb.webapp.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    String upload(MultipartFile file, String fileName);

    void delete(String storageUrl);

    String rename(String oldStorageUrl, String newFileName);

    Resource loadAsResource(String storageUrl);
}
