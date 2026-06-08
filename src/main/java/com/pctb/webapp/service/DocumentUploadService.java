package com.pctb.webapp.service;

import com.pctb.webapp.dto.response.DocumentUploadResponse;
import com.pctb.webapp.entity.Document;

import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.exception.UploadStatus;
import com.pctb.webapp.repository.DocumentRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentUploadService {
    DocumentRepo documentRepo;

    UserRepo userRepo;

    FileValidationService fileValidationService;

    StorageService storageService;

    @Value("${app.upload.max-user-storage}")
    @NonFinal
    long maxUserStorage;

    // Upload document
    public DocumentUploadResponse upload(
            MultipartFile file,  // Đại diện cho file
            Boolean replaceExisting, // Có cho phép thay thế file đã tồn tại hay không
            JwtAuthenticationToken authentication // token authen
    ) {
        // Lấy user id
        String userId = authentication.getName();
        //
        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra mimeType thật của file
        String realMimeType = fileValidationService.validate(file);
        // Lấy tên gốc của file
        String originalFileName = cleanOriginalFileName(file.getOriginalFilename());
        // Lấy extension từ tên file gốc
        String extension = fileValidationService.getExtension(originalFileName);
        // Physical storage path uses UUID to avoid filename conflicts.
        String storedFileName = buildStoredFileName(owner.getId(), extension);

        boolean shouldReplace = Boolean.TRUE.equals(replaceExisting); // Kiểm tra có cho phép thay đổi không

        // Tìm document đã tồn tại chưa
        Document existingDocument = documentRepo.findByOwnerAndFileName(owner, originalFileName)
                .orElse(null);
        // Nếu đã tồn tại và không cho phép thay đổi thì báo lỗi
        if (existingDocument != null && !shouldReplace) {
            throw new AppException(ErrorCode.FILE_ALREADY_EXISTS);
        }

        validateStorageCapacity(owner, existingDocument, file.getSize());

        // Nếu đã tồn tại và cho phép thay đổi thì xóa file cũ trong storage và DB
        if (existingDocument != null) {
            storageService.delete(existingDocument.getStorageUrl());
            documentRepo.delete(existingDocument);
        }
        // Lấy url lưu trữ
        String storageUrl = storageService.upload(file, storedFileName);

        LocalDateTime now = LocalDateTime.now();

        Document document = Document.builder()
                .title(originalFileName)
                .fileName(originalFileName)
                .fileExtension(extension)
                .mimeType(realMimeType)
                .fileSize(file.getSize())
                .storageUrl(storageUrl)
                .status(UploadStatus.COMPLETED)
                .owner(owner)
                .createdAt(now)
                .updatedAt(now)
                .deleted(false)
                .build();

        document = documentRepo.save(document);

        return DocumentUploadResponse.builder()
                .documentId(document.getId())
                .fileName(document.getFileName())
                .fileExtension(document.getFileExtension())
                .mimeType(document.getMimeType())
                .fileSize(document.getFileSize())
                .storageUrl(document.getStorageUrl())
                .viewUrl(buildDocumentViewUrl(document.getId()))
                .downloadUrl(buildDocumentDownloadUrl(document.getId()))
                .status(document.getStatus().name())
                .uploadedAt(document.getCreatedAt().toString())
                .timeSinceUpload(formatTimeSinceUpload(document.getCreatedAt()))
                .build();
    }
    // Lấy tên gốc của file, ví dụ name.pdf thay vì nguyên path
    private String buildDocumentViewUrl(String documentId) {
        return "/documents/" + documentId + "/view";
    }

    private String buildDocumentDownloadUrl(String documentId) {
        return "/documents/" + documentId + "/download";
    }

    private String cleanOriginalFileName(String originalFileName) {
        String cleanPath = StringUtils.cleanPath(originalFileName);
        int slashIndex = Math.max(cleanPath.lastIndexOf('/'), cleanPath.lastIndexOf('\\'));

        if (slashIndex >= 0) {
            return cleanPath.substring(slashIndex + 1);
        }

        return cleanPath;
    }

    private String buildStoredFileName(String userId, String extension) {
        return "documents/" + userId + "/" + UUID.randomUUID() + "." + extension;
    }

    private String formatTimeSinceUpload(LocalDateTime uploadedAt) {
        if (uploadedAt == null) {
            return null;
        }

        Duration duration = Duration.between(uploadedAt, LocalDateTime.now());
        if (duration.isNegative() || duration.getSeconds() < 5) {
            return "v\u1eeba xong";
        }

        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + " gi\u00e2y tr\u01b0\u1edbc";
        }

        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + " ph\u00fat tr\u01b0\u1edbc";
        }

        long hours = duration.toHours();
        if (hours < 24) {
            return hours + " gi\u1edd tr\u01b0\u1edbc";
        }

        long days = duration.toDays();
        if (days < 30) {
            return days + " ng\u00e0y tr\u01b0\u1edbc";
        }

        if (days < 365) {
            return days / 30 + " th\u00e1ng tr\u01b0\u1edbc";
        }

        return days / 365 + " n\u0103m tr\u01b0\u1edbc";
    }

    private void validateStorageCapacity(User owner, Document existingDocument, long newFileSize) {
        Long usedStorageResult = documentRepo.sumFileSizeByOwner(owner);
        long usedStorage = usedStorageResult == null ? 0 : usedStorageResult;
        long replacedFileSize = existingDocument == null || existingDocument.getFileSize() == null
                ? 0
                : existingDocument.getFileSize();
        long projectedStorage = usedStorage - replacedFileSize + newFileSize;

        if (projectedStorage > maxUserStorage) {
            throw new AppException(ErrorCode.STORAGE_NOT_ENOUGH);
        }
    }
}
