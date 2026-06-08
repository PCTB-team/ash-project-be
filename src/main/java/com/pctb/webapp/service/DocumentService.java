package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.UpdateDocumentRequest;
import com.pctb.webapp.dto.response.DeleteDocumentResponse;
import com.pctb.webapp.dto.response.DocumentResponse;
import com.pctb.webapp.dto.response.DownloadDocumentResponse;
import com.pctb.webapp.entity.Document;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.DocumentRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentService {
    DocumentRepo documentRepo;

    UserRepo userRepo;

    StorageService storageService;

    FileValidationService fileValidationService;

    // Lay danh sach document cua user dang dang nhap
    public List<DocumentResponse> getMyDocuments(JwtAuthenticationToken authentication) {
        // Lay userId tu access token
        String userId = authentication.getName();

        // Tim user trong database
        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Lay danh sach document chua bi xoa, file moi nhat nam tren dau
        List<Document> documents = documentRepo.findActiveByOwner(owner);

        // Chuyen tung Document Entity sang DocumentResponse DTO
        List<DocumentResponse> documentResponses = new ArrayList<>();

        for (Document document : documents) {
            documentResponses.add(buildDocumentResponse(document));
        }

        return documentResponses;
    }

    // Dua document cua user dang dang nhap vao thung rac
    public DeleteDocumentResponse deleteMyDocument(String documentId, JwtAuthenticationToken authentication) {
        // Lay userId tu access token
        String userId = authentication.getName();

        // Tim user trong database
        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Tim document theo documentId
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        // Kiem tra document co thuoc user dang dang nhap khong
        String documentOwnerId = document.getOwner().getId();
        if (!owner.getId().equals(documentOwnerId)) {
            throw new AppException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        // Neu document chua nam trong thung rac thi danh dau la da xoa
        if (!Boolean.TRUE.equals(document.getDeleted())) {
            document.setDeleted(true);
            document.setDeletedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            documentRepo.save(document);
        }

        return DeleteDocumentResponse.builder()
                .documentId(documentId)
                .deleted(true)
                .build();
    }

    // Lay danh sach document dang nam trong thung rac cua user dang dang nhap
    public List<DocumentResponse> getMyTrashDocuments(JwtAuthenticationToken authentication) {
        // Lay userId tu access token
        String userId = authentication.getName();

        // Tim user trong database
        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Lay danh sach document da bi xoa tức là lấy dững document có deleted is true
        List<Document> documents = documentRepo.findTrashByOwner(owner);

        // Chuyen tung Document Entity sang DocumentResponse DTO
        List<DocumentResponse> documentResponses = new ArrayList<>();

        for (Document document : documents) {
            documentResponses.add(buildDocumentResponse(document));
        }

        return documentResponses;
    }

    // Khoi phuc document tu thung rac
    public DocumentResponse restoreMyDocument(String documentId, JwtAuthenticationToken authentication) {
        // Lay userId tu access token
        String userId = authentication.getName();

        // Tim user trong database
        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Tim document theo documentId
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        // Kiem tra document co thuoc user dang dang nhap khong
        String documentOwnerId = document.getOwner().getId();
        if (!owner.getId().equals(documentOwnerId)) {
            throw new AppException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        // Restore chi ap dung cho document dang nam trong thung rac
        if (!Boolean.TRUE.equals(document.getDeleted())) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_IN_TRASH);
        }

        // Dua document ra khoi thung rac
        document.setDeleted(false);

        // Xoa thoi diem bi xoa vi document da duoc khoi phuc
        document.setDeletedAt(null);

        // Cap nhat thoi gian thay doi metadata
        document.setUpdatedAt(LocalDateTime.now());

        // Luu metadata moi xuong database
        document = documentRepo.save(document);

        return buildDocumentResponse(document);
    }

    // Xoa vinh vien document dang nam trong thung rac
    public DeleteDocumentResponse deleteMyDocumentPermanently(
            String documentId,
            JwtAuthenticationToken authentication
    ) {
        // Lay userId tu access token
        String userId = authentication.getName();

        // Tim user trong database
        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Tim document theo documentId
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        // Kiem tra document co thuoc user dang dang nhap khong
        String documentOwnerId = document.getOwner().getId();
        if (!owner.getId().equals(documentOwnerId)) {
            throw new AppException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        // Chi cho phep xoa vinh vien document dang nam trong thung rac
        if (!Boolean.TRUE.equals(document.getDeleted())) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_IN_TRASH);
        }

        // Xoa file vat ly trong storage
        storageService.delete(document.getStorageUrl());

        // Xoa metadata trong database
        documentRepo.delete(document);

        return DeleteDocumentResponse.builder()
                .documentId(documentId)
                .deleted(true)
                .build();
    }

    // Tai document cua user dang dang nhap xuong may
    public DownloadDocumentResponse downloadMyDocument(
            String documentId,
            JwtAuthenticationToken authentication
    ) {
        // Lay userId tu access token
        String userId = authentication.getName();

        // Tim user trong database
        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Tim document theo documentId
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        // Kiem tra document co thuoc user dang dang nhap khong
        String documentOwnerId = document.getOwner().getId();
        if (!owner.getId().equals(documentOwnerId)) {
            throw new AppException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        // Khong cho tai document dang nam trong thung rac
        if (Boolean.TRUE.equals(document.getDeleted())) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        // Doc file vat ly tu storage
        org.springframework.core.io.Resource resource = storageService.loadAsResource(document.getStorageUrl());

        return DownloadDocumentResponse.builder()
                .fileName(document.getFileName())
                .mimeType(document.getMimeType())
                .resource(resource)
                .build();
    }

    // Sua thong tin document cua user dang dang nhap
    public DocumentResponse updateMyDocument(
            String documentId,
            UpdateDocumentRequest request,
            JwtAuthenticationToken authentication
    ) {
        // Lay userId tu access token
        String userId = authentication.getName();

        // Tim user trong database
        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Tim document theo documentId
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        // Kiem tra document co thuoc user dang dang nhap khong
        String documentOwnerId = document.getOwner().getId();
        if (!owner.getId().equals(documentOwnerId)) {
            throw new AppException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        if (Boolean.TRUE.equals(document.getDeleted())) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        // Lay ten file moi va giu extension cu neu user khong nhap extension
        String newFileName = buildUpdatedFileName(request.getFileName(), document);
        String newExtension = fileValidationService.getExtension(newFileName);

        // Neu ten moi giong ten cu thi tra ve thong tin hien tai
        if (document.getFileName().equals(newFileName)) {
            return buildDocumentResponse(document);
        }

        // Kiem tra user da co file cung ten moi chua
        Document existingDocument = documentRepo.findByOwnerAndFileName(owner, newFileName)
                .orElse(null);
        if (existingDocument != null) {
            throw new AppException(ErrorCode.FILE_ALREADY_EXISTS);
        }

        // Physical file uses a separate storage key, so rename only updates display/download metadata.
        document.setTitle(newFileName);
        document.setFileName(newFileName);
        document.setFileExtension(newExtension);
        document.setUpdatedAt(LocalDateTime.now());

        document = documentRepo.save(document);

        return buildDocumentResponse(document);
    }

    private String cleanFileName(String fileName) {
        String cleanPath = StringUtils.cleanPath(fileName);
        int slashIndex = Math.max(cleanPath.lastIndexOf('/'), cleanPath.lastIndexOf('\\'));

        if (slashIndex >= 0) {
            return cleanPath.substring(slashIndex + 1);
        }

        return cleanPath;
    }

    private String buildUpdatedFileName(String inputFileName, Document document) {
        String cleanFileName = cleanFileName(inputFileName).trim();

        if (cleanFileName.isBlank()) {
            throw new AppException(ErrorCode.DOCUMENT_FILE_NAME_REQUIRED);
        }

        if (!cleanFileName.contains(".")) {
            return cleanFileName + "." + document.getFileExtension();
        }

        String newExtension = fileValidationService.getExtension(cleanFileName);
        if (!document.getFileExtension().equalsIgnoreCase(newExtension)) {
            throw new AppException(ErrorCode.DOCUMENT_EXTENSION_CANNOT_CHANGE);
        }

        return cleanFileName;
    }

    private DocumentResponse buildDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .documentId(document.getId())
                .fileName(document.getFileName())
                .fileExtension(document.getFileExtension())
                .mimeType(document.getMimeType())
                .fileSize(document.getFileSize())
                .storageUrl(document.getStorageUrl())
                .viewUrl(buildDocumentViewUrl(document.getId()))
                .downloadUrl(buildDocumentDownloadUrl(document.getId()))
                .status(document.getStatus().name())
                .uploadedAt(document.getCreatedAt() == null ? null : document.getCreatedAt().toString())
                .timeSinceUpload(formatTimeSinceUpload(document.getCreatedAt()))
                .deleted(Boolean.TRUE.equals(document.getDeleted()))
                .deletedAt(document.getDeletedAt() == null ? null : document.getDeletedAt().toString())
                .build();
    }

    private String buildDocumentViewUrl(String documentId) {
        return "/documents/" + documentId + "/view";
    }

    private String buildDocumentDownloadUrl(String documentId) {
        return "/documents/" + documentId + "/download";
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
}
