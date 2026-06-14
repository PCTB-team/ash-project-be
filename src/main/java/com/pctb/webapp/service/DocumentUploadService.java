package com.pctb.webapp.service;

import com.pctb.webapp.dto.response.DocumentUploadResponse;
import com.pctb.webapp.entity.Document;
import com.pctb.webapp.entity.Folder;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.exception.UploadStatus;
import com.pctb.webapp.repository.DocumentRepo;
import com.pctb.webapp.repository.FolderRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    FolderRepo folderRepo;

    FileValidationService fileValidationService;

    StorageService storageService;

    @Value("${app.upload.max-user-storage}")
    @NonFinal
    long maxUserStorage;

    // Xử lý toàn bộ luồng upload tài liệu cá nhân: xác thực user, kiểm tra file, xử lý replace, lưu storage, lưu DB và trả DTO.
    @Transactional
    // Bọc upload trong transaction để dữ liệu Document và dung lượng Folder được cập nhật đồng bộ.
    public DocumentUploadResponse upload(
            MultipartFile file,
            Boolean replaceExisting,
            String folderId,
            JwtAuthenticationToken authentication
    ) {
        String userId = authentication.getName();
        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        String normalizedFolderId = normalizeOptionalId(folderId);
        Folder folder = resolveFolder(normalizedFolderId, userId);

        String realMimeType = fileValidationService.validate(file);
        String originalFileName = cleanOriginalFileName(file.getOriginalFilename());
        String extension = fileValidationService.getExtension(originalFileName);
        String storedFileName = buildStoredFileName(owner.getId(), extension);

        boolean shouldReplace = Boolean.TRUE.equals(replaceExisting);
        Document existingDocument = documentRepo.findByOwnerIdAndFolderIdAndFileName(owner.getId(), normalizedFolderId, originalFileName)
                .orElse(null);

        if (existingDocument != null && !shouldReplace) {
            throw new AppException(ErrorCode.FILE_ALREADY_EXISTS);
        }

        validateStorageCapacity(owner, existingDocument, file.getSize());

        if (existingDocument != null) {
            storageService.delete(existingDocument.getStorageUrl());
            if (!Boolean.TRUE.equals(existingDocument.getDeleted())) {
                updateFolderSizeCascade(existingDocument.getFolder(), -safeFileSize(existingDocument));
            }
            documentRepo.delete(existingDocument);
        }

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
                .folder(folder)
                .createdAt(now)
                .updatedAt(now)
                .deleted(false)
                .build();

        document = documentRepo.save(document);
        updateFolderSizeCascade(folder, file.getSize());

        return DocumentUploadResponse.builder()
                .documentId(document.getId())
                .fileName(document.getFileName())
                .fileExtension(document.getFileExtension())
                .mimeType(document.getMimeType())
                .fileSize(document.getFileSize())
                .storageUrl(document.getStorageUrl())
                .folderId(document.getFolder() == null ? null : document.getFolder().getId())
                .viewUrl(buildDocumentViewUrl(document.getId()))
                .downloadUrl(buildDocumentDownloadUrl(document.getId()))
                .status(document.getStatus().name())
                .uploadedAt(document.getCreatedAt().toString())
                .timeSinceUpload(formatTimeSinceUpload(document.getCreatedAt()))
                .build();
    }

    // Tìm folder đích theo id và user; nếu folderId null thì upload vào thư mục gốc.
    private Folder resolveFolder(String folderId, String userId) {
        if (folderId == null) {
            return null;
        }

        return folderRepo.findActiveByIdAndOwnerId(folderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FOLDER_NOT_FOUND));
    }

    // Cộng hoặc trừ dung lượng của folder hiện tại và toàn bộ folder cha để thống kê size luôn đúng.
    private void updateFolderSizeCascade(Folder folder, long delta) {
        Folder current = folder;

        while (current != null) {
            long currentSize = current.getSize() == null ? 0 : current.getSize();
            current.setSize(Math.max(0, currentSize + delta));
            current.setUpdatedAt(LocalDateTime.now());
            folderRepo.save(current);
            current = current.getParent();
        }
    }

    // Lấy fileSize an toàn, tránh lỗi null khi tài liệu cũ thiếu dữ liệu dung lượng.
    private long safeFileSize(Document document) {
        return document.getFileSize() == null ? 0 : document.getFileSize();
    }

    // Chuẩn hóa id optional: chuỗi rỗng được xem là null, chuỗi có dữ liệu thì trim.
    private String normalizeOptionalId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        return id.trim();
    }

    // Tạo URL xem tài liệu theo convention API hiện tại.
    private String buildDocumentViewUrl(String documentId) {
        return "/documents/" + documentId + "/view";
    }

    // Tạo URL tải tài liệu theo convention API hiện tại.
    private String buildDocumentDownloadUrl(String documentId) {
        return "/documents/" + documentId + "/download";
    }

    // Làm sạch tên file gốc, loại bỏ path nếu client gửi kèm đường dẫn cục bộ.
    private String cleanOriginalFileName(String originalFileName) {
        String cleanPath = StringUtils.cleanPath(originalFileName);
        int slashIndex = Math.max(cleanPath.lastIndexOf('/'), cleanPath.lastIndexOf('\\'));

        if (slashIndex >= 0) {
            return cleanPath.substring(slashIndex + 1);
        }

        return cleanPath;
    }

    // Tạo tên file lưu trong storage theo userId và UUID để tránh trùng tên vật lý.
    private String buildStoredFileName(String userId, String extension) {
        return "documents/" + userId + "/" + UUID.randomUUID() + "." + extension;
    }

    // Chuyển thời điểm upload thành chuỗi tương đối như "vừa xong", "3 phút trước".
    private String formatTimeSinceUpload(LocalDateTime uploadedAt) {
        if (uploadedAt == null) {
            return null;
        }

        Duration duration = Duration.between(uploadedAt, LocalDateTime.now());
        if (duration.isNegative() || duration.getSeconds() < 5) {
            return "vừa xong";
        }

        long seconds = duration.getSeconds();
        if (seconds < 60) {
            return seconds + " giây trước";
        }

        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return minutes + " phút trước";
        }

        long hours = duration.toHours();
        if (hours < 24) {
            return hours + " giờ trước";
        }

        long days = duration.toDays();
        if (days < 30) {
            return days + " ngày trước";
        }

        if (days < 365) {
            return days / 30 + " tháng trước";
        }

        return days / 365 + " năm trước";
    }

    // Kiểm tra tổng dung lượng sau upload hoặc replace có vượt giới hạn storage của user hay không.
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
