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

import java.time.LocalDateTime;

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
            MultipartFile file,
            Boolean replaceExisting,
            JwtAuthenticationToken authentication
    ) {
        // Lấy user id
        String userId = authentication.getName();

        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Sẽ kiểm tra mimeType của file và lấy ra mimeType
        String realMimeType = fileValidationService.validate(file);
        // Lấy ra tên gốc của file
        String originalFileName = cleanOriginalFileName(file.getOriginalFilename());
        // Kiểm tra xem extension của file từ name của file gốc
        String extension = fileValidationService.getExtension(originalFileName);
        //Tên file sẽ lưu
        String storedFileName = buildStoredFileName(originalFileName);

        boolean shouldReplace = Boolean.TRUE.equals(replaceExisting); // Coi có cho phép thay đổi không

        // Tìm coi document đã tồn tại chưa
        Document existingDocument = documentRepo.findByOwnerAndFileName(owner, originalFileName)
                .orElse(null);
        // Nếu đã tồn tại và cho phép thay đổi đang false
        if (existingDocument != null && !shouldReplace) {
            throw new AppException(ErrorCode.FILE_ALREADY_EXISTS);
        }

        validateStorageCapacity(owner, existingDocument, file.getSize());

        // Nếu đã tồn tại và cho phép thay đổi thì sẽ xóa trong storageService và xóa trong DB
        if (existingDocument != null) {
            storageService.delete(existingDocument.getStorageUrl());
            documentRepo.delete(existingDocument);
        }
        // Lấy url
        String storageUrl = storageService.upload(file, storedFileName);

        Document document = Document.builder()
                .title(originalFileName)
                .fileName(originalFileName)
                .fileExtension(extension)
                .mimeType(realMimeType)
                .fileSize(file.getSize())
                .storageUrl(storageUrl)
                .status(UploadStatus.COMPLETED)
                .owner(owner)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
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
                .status(document.getStatus().name())
                .build();
    }
    // Lấy tên gốc của file vidu name.pdf chứ không lấy nguyên 1 path
    private String cleanOriginalFileName(String originalFileName) {
        String cleanPath = StringUtils.cleanPath(originalFileName);
        int slashIndex = Math.max(cleanPath.lastIndexOf('/'), cleanPath.lastIndexOf('\\'));

        if (slashIndex >= 0) {
            return cleanPath.substring(slashIndex + 1);
        }

        return cleanPath;
    }

    private String buildStoredFileName(String originalFileName) {
        return originalFileName;
    }
    // Kiểm tra xem khi upload file thì dung lượng project có vượt quá dung lượng cho phép của User không
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
