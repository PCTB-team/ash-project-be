package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.UpdateDocumentRequest;
import com.pctb.webapp.dto.response.DeleteDocumentResponse;
import com.pctb.webapp.dto.response.DocumentResponse;
import com.pctb.webapp.dto.response.DownloadDocumentResponse;
import com.pctb.webapp.dto.response.FilteredDocumentResponse;
import com.pctb.webapp.entity.Document;
import com.pctb.webapp.entity.Folder;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.DocumentRepo;
import com.pctb.webapp.repository.FolderRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentService {
    DocumentRepo documentRepo;

    UserRepo userRepo;

    FolderRepo folderRepo;

    StorageService storageService;

    FileValidationService fileValidationService;

    // Lấy toàn bộ tài liệu active của user hiện tại, sắp xếp theo thời gian tạo mới nhất.
    public List<DocumentResponse> getMyDocuments(JwtAuthenticationToken authentication) {
        String userId = authentication.getName();

        List<Document> documents = documentRepo.findActiveByOwnerId(userId);
        List<DocumentResponse> documentResponses = new ArrayList<>();

        for (Document document : documents) {
            documentResponses.add(buildDocumentResponse(document));
        }

        return documentResponses;
    }

    // Lấy tài liệu active của user theo phân trang và folder hiện tại để frontend render danh sách lớn.
    public Page<DocumentResponse> getMyDocumentsPage(
            JwtAuthenticationToken authentication,
            int page,
            int size,
            String folderId
    ) {
        String userId = authentication.getName();
        String normalizedFolderId = normalizeOptionalId(folderId);
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);

        if (normalizedFolderId != null) {
            resolveFolder(normalizedFolderId, userId);
        }

        return documentRepo.findActiveByOwnerIdAndFolderId(
                        userId,
                        normalizedFolderId,
                        PageRequest.of(normalizedPage, normalizedSize)
                )
                .map(this::buildDocumentResponse);
    }

    public Page<DocumentResponse> searchMyDocuments(
            JwtAuthenticationToken authentication,
            String keyword,
            int page,
            int size,
            String folderId
    ) {
        String userId = authentication.getName();
        String normalizedFolderId = normalizeOptionalId(folderId);
        String normalizedKeyword = normalizeOptionalKeyword(keyword);
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);

        if (normalizedFolderId != null) {
            resolveFolder(normalizedFolderId, userId);
        }

        return documentRepo.searchActiveByOwnerIdAndFileName(
                        userId,
                        normalizedFolderId,
                        normalizedKeyword,
                        PageRequest.of(normalizedPage, normalizedSize)
                )
                .map(this::buildDocumentResponse);
    }

    // Lọc tài liệu của user theo nhóm loại file như document, image, audio, video hoặc presentation.
    public FilteredDocumentResponse filterMyDocumentsByFileType(
            JwtAuthenticationToken authentication,
            String fileType,
            String folderId
    ) {
        String userId = authentication.getName();
        String normalizedFolderId = normalizeOptionalId(folderId);

        if (normalizedFolderId != null) {
            resolveFolder(normalizedFolderId, userId);
        }

        List<String> extensions = resolveFileTypeExtensions(fileType);
        List<DocumentResponse> documents = documentRepo
                .findActiveByOwnerIdAndFolderIdAndFileExtensions(userId, normalizedFolderId, extensions)
                .stream()
                .map(this::buildDocumentResponse)
                .toList();
        long total = documentRepo.countActiveByOwnerIdAndFolderIdAndFileExtensions(
                userId,
                normalizedFolderId,
                extensions
        );

        return FilteredDocumentResponse.builder()
                .fileType(normalizeFileType(fileType))
                .total(total)
                .documents(documents)
                .build();
    }

    // Lọc tài liệu theo folder, fileType và keyword; hỗ trợ phân trang cho màn hình quản lý file.
    public FilteredDocumentResponse filterMyDocumentFiles(
            JwtAuthenticationToken authentication,
            String folderId
    ) {
        String userId = authentication.getName();
        String normalizedFolderId = normalizeOptionalId(folderId);

        if (normalizedFolderId != null) {
            resolveFolder(normalizedFolderId, userId);
        }

        List<String> documentExtensions = List.of("doc", "docx", "pdf", "xls", "xlsx", "ppt", "pptx", "txt");
        List<DocumentResponse> documents = documentRepo
                .findActiveByOwnerIdAndFolderIdAndFileExtensions(userId, normalizedFolderId, documentExtensions)
                .stream()
                .map(this::buildDocumentResponse)
                .toList();
        long total = documentRepo.countActiveByOwnerIdAndFolderIdAndFileExtensions(
                userId,
                normalizedFolderId,
                documentExtensions
        );

        return FilteredDocumentResponse.builder()
                .fileType("document")
                .total(total)
                .documents(documents)
                .build();
    }

    @Transactional
    // Đưa tài liệu của user vào thùng rác bằng soft delete và cập nhật lại dung lượng folder liên quan.
    public DeleteDocumentResponse deleteMyDocument(String documentId, JwtAuthenticationToken authentication) {
        String userId = authentication.getName();

        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        String documentOwnerId = document.getOwner().getId();
        if (!owner.getId().equals(documentOwnerId)) {
            throw new AppException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        if (!Boolean.TRUE.equals(document.getDeleted())) {
            document.setDeleted(true);
            document.setDeletedAt(LocalDateTime.now());
            document.setUpdatedAt(LocalDateTime.now());
            updateFolderSizeCascade(document.getFolder(), -safeFileSize(document));
            documentRepo.save(document);
        }

        return DeleteDocumentResponse.builder()
                .documentId(documentId)
                .deleted(true)
                .build();
    }

    // Lấy danh sách tài liệu đang nằm trong thùng rác của user hiện tại.
    public List<DocumentResponse> getMyTrashDocuments(JwtAuthenticationToken authentication) {
        String userId = authentication.getName();

        List<Document> documents = documentRepo.findTrashByOwnerId(userId);
        List<DocumentResponse> documentResponses = new ArrayList<>();

        for (Document document : documents) {
            documentResponses.add(buildDocumentResponse(document));
        }

        return documentResponses;
    }

    @Transactional
    // Khôi phục tài liệu từ thùng rác về trạng thái active và cộng lại dung lượng folder.
    public DocumentResponse restoreMyDocument(String documentId, JwtAuthenticationToken authentication) {
        String userId = authentication.getName();

        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        String documentOwnerId = document.getOwner().getId();
        if (!owner.getId().equals(documentOwnerId)) {
            throw new AppException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        if (!Boolean.TRUE.equals(document.getDeleted())) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_IN_TRASH);
        }

        document.setDeleted(false);
        document.setDeletedAt(null);
        document.setUpdatedAt(LocalDateTime.now());
        updateFolderSizeCascade(document.getFolder(), safeFileSize(document));

        document = documentRepo.save(document);

        return buildDocumentResponse(document);
    }

    @Transactional
    // Xóa vĩnh viễn tài liệu của user khỏi storage và database, thường dùng khi dọn thùng rác.
    public DeleteDocumentResponse deleteMyDocumentPermanently(
            String documentId,
            JwtAuthenticationToken authentication
    ) {
        String userId = authentication.getName();

        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        String documentOwnerId = document.getOwner().getId();
        if (!owner.getId().equals(documentOwnerId)) {
            throw new AppException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        if (!Boolean.TRUE.equals(document.getDeleted())) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_IN_TRASH);
        }

        storageService.delete(document.getStorageUrl());
        documentRepo.delete(document);

        return DeleteDocumentResponse.builder()
                .documentId(documentId)
                .deleted(true)
                .build();
    }

    // Kiểm tra quyền sở hữu rồi trả thông tin download/resource để controller stream file cho client.
    public DownloadDocumentResponse downloadMyDocument(
            String documentId,
            JwtAuthenticationToken authentication
    ) {
        String userId = authentication.getName();

        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        String documentOwnerId = document.getOwner().getId();
        if (!owner.getId().equals(documentOwnerId)) {
            throw new AppException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        if (Boolean.TRUE.equals(document.getDeleted())) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        org.springframework.core.io.Resource resource = storageService.loadAsResource(document.getStorageUrl());

        return DownloadDocumentResponse.builder()
                .fileName(document.getFileName())
                .mimeType(document.getMimeType())
                .resource(resource)
                .build();
    }

    @Transactional
    // Cập nhật tên tài liệu và/hoặc folder chứa tài liệu, đồng thời rename file trong storage nếu tên đổi.
    public DocumentResponse updateMyDocument(
            String documentId,
            UpdateDocumentRequest request,
            JwtAuthenticationToken authentication
    ) {
        String userId = authentication.getName();

        User owner = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        String documentOwnerId = document.getOwner().getId();
        if (!owner.getId().equals(documentOwnerId)) {
            throw new AppException(ErrorCode.DOCUMENT_ACCESS_DENIED);
        }

        if (Boolean.TRUE.equals(document.getDeleted())) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        String newFileName = buildUpdatedFileName(request.getFileName(), document);
        String newExtension = fileValidationService.getExtension(newFileName);

        if (document.getFileName().equals(newFileName)) {
            return buildDocumentResponse(document);
        }

        String folderId = document.getFolder() == null ? null : document.getFolder().getId();
        Document existingDocument = documentRepo.findByOwnerIdAndFolderIdAndFileName(owner.getId(), folderId, newFileName)
                .orElse(null);
        if (existingDocument != null && !existingDocument.getId().equals(document.getId())) {
            throw new AppException(ErrorCode.FILE_ALREADY_EXISTS);
        }

        document.setTitle(newFileName);
        document.setFileName(newFileName);
        document.setFileExtension(newExtension);
        document.setUpdatedAt(LocalDateTime.now());

        document = documentRepo.save(document);

        return buildDocumentResponse(document);
    }

    // Tìm folder theo id và user; null nghĩa là tài liệu nằm ở root.
    private Folder resolveFolder(String folderId, String userId) {
        return folderRepo.findActiveByIdAndOwnerId(folderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FOLDER_NOT_FOUND));
    }

    // Cập nhật dung lượng folder hiện tại và toàn bộ folder cha khi file bị thêm, xóa, restore hoặc di chuyển.
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

    // Lấy fileSize an toàn, trả về 0 nếu dữ liệu cũ bị null.
    private long safeFileSize(Document document) {
        return document.getFileSize() == null ? 0 : document.getFileSize();
    }

    // Chuẩn hóa id optional: null hoặc blank thành null, còn giá trị hợp lệ thì trim.
    private String normalizeOptionalId(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }

        return id.trim();
    }

    private String normalizeOptionalKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }

        return keyword.trim();
    }

    // Chuyển fileType từ request thành danh sách extension tương ứng để query database.
    private List<String> resolveFileTypeExtensions(String fileType) {
        String normalizedFileType = normalizeFileType(fileType);

        return switch (normalizedFileType) {
            case "word", "doc", "docx" -> List.of("doc", "docx");
            case "pdf" -> List.of("pdf");
            case "image", "photo", "picture", "png", "jpg", "jpeg" -> List.of("png", "jpg", "jpeg");
            case "powerpoint", "presentation", "ppt", "pptx" -> List.of("ppt", "pptx");
            case "excel", "spreadsheet", "xls", "xlsx" -> List.of("xls", "xlsx");
            case "text", "txt" -> List.of("txt");
            case "audio", "music", "mp3" -> List.of("mp3");
            case "video", "mp4" -> List.of("mp4");
            default -> throw new AppException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        };
    }

    // Chuẩn hóa fileType về chữ thường và kiểm tra có thuộc nhóm được hỗ trợ hay không.
    private String normalizeFileType(String fileType) {
        if (fileType == null || fileType.isBlank()) {
            throw new AppException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }

        String normalizedFileType = fileType.trim().toLowerCase(Locale.ROOT);
        if (normalizedFileType.startsWith(".")) {
            normalizedFileType = normalizedFileType.substring(1);
        }

        if (normalizedFileType.isBlank()) {
            throw new AppException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }

        return normalizedFileType;
    }

    // Làm sạch tên file người dùng nhập, chặn tên rỗng và loại bỏ path client gửi kèm.
    private String cleanFileName(String fileName) {
        String cleanPath = StringUtils.cleanPath(fileName);
        int slashIndex = Math.max(cleanPath.lastIndexOf('/'), cleanPath.lastIndexOf('\\'));

        if (slashIndex >= 0) {
            return cleanPath.substring(slashIndex + 1);
        }

        return cleanPath;
    }

    // Tạo tên file mới khi update, giữ extension cũ nếu người dùng chỉ nhập phần tên không có đuôi.
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

    // Chuyển entity Document sang DTO trả về cho frontend, kèm URL xem/tải và thời gian tương đối.
    private DocumentResponse buildDocumentResponse(Document document) {
        return DocumentResponse.builder()
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
                .uploadedAt(document.getCreatedAt() == null ? null : document.getCreatedAt().toString())
                .timeSinceUpload(formatTimeSinceUpload(document.getCreatedAt()))
                .deleted(Boolean.TRUE.equals(document.getDeleted()))
                .deletedAt(document.getDeletedAt() == null ? null : document.getDeletedAt().toString())
                .build();
    }

    // Tạo URL xem tài liệu theo convention API hiện tại.
    private String buildDocumentViewUrl(String documentId) {
        return "/documents/" + documentId + "/view";
    }

    // Tạo URL tải tài liệu theo convention API hiện tại.
    private String buildDocumentDownloadUrl(String documentId) {
        return "/documents/" + documentId + "/download";
    }

    // Chuyển thời điểm upload thành chuỗi tương đối như "vừa xong", "3 phút trước".
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
