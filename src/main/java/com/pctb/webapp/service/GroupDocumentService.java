package com.pctb.webapp.service;

import com.pctb.webapp.dto.response.DeleteDocumentResponse;
import com.pctb.webapp.dto.response.DocumentResponse;
import com.pctb.webapp.dto.response.DownloadDocumentResponse;
import com.pctb.webapp.entity.Document;
import com.pctb.webapp.entity.GroupMember;
import com.pctb.webapp.entity.GroupRole;
import com.pctb.webapp.entity.StudyGroup;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.exception.UploadStatus;
import com.pctb.webapp.repository.DocumentRepo;
import com.pctb.webapp.repository.GroupMemberRepo;
import com.pctb.webapp.repository.StudyGroupRepo;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupDocumentService {
    DocumentRepo documentRepo;
    StudyGroupRepo groupRepo;
    GroupMemberRepo memberRepo;
    UserRepo userRepo;
    FileValidationService fileValidationService;
    StorageService storageService;

    @Value("${app.upload.max-user-storage}")
    @NonFinal
    long maxUserStorage;

    // Member upload file khi da duoc leader cap quyen.
    @Transactional
    public DocumentResponse uploadDocument(
            String groupId,
            MultipartFile file,
            JwtAuthenticationToken authentication
    ) {
        User uploader = getCurrentUser(authentication);
        StudyGroup group = getGroup(groupId);
        GroupMember membership = getMembership(group, uploader);
        checkUploadPermission(membership);

        String realMimeType = fileValidationService.validate(file);
        String originalFileName = cleanOriginalFileName(file.getOriginalFilename());
        String extension = fileValidationService.getExtension(originalFileName);
        validateStorageCapacity(uploader, file.getSize());

        String storageFileName = buildGroupStoredFileName(group.getId(), originalFileName);
        String storageUrl = storageService.upload(file, storageFileName);

        try {
            Document document = Document.builder()
                    .title(originalFileName)
                    .fileName(originalFileName)
                    .fileExtension(extension)
                    .mimeType(realMimeType)
                    .fileSize(file.getSize())
                    .storageUrl(storageUrl)
                    .status(UploadStatus.COMPLETED)
                    .owner(uploader)
                    .group(group)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .deleted(false)
                    .build();

            return buildDocumentResponse(documentRepo.save(document));
        } catch (RuntimeException exception) {
            deleteStorageQuietly(storageUrl);
            throw exception;
        }
    }

    // Member trong group xem file da upload.
    public List<DocumentResponse> getGroupDocuments(
            String groupId,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        StudyGroup group = getGroup(groupId);
        getMembership(group, currentUser);

        return documentRepo.findActiveByGroup(group)
                .stream()
                .map(this::buildDocumentResponse)
                .toList();
    }

    // Member trong group tai file ve.
    public DownloadDocumentResponse downloadGroupDocument(
            String groupId,
            String documentId,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        StudyGroup group = getGroup(groupId);
        getMembership(group, currentUser);
        Document document = getGroupDocument(group, documentId);

        return DownloadDocumentResponse.builder()
                .fileName(document.getFileName())
                .mimeType(document.getMimeType())
                .resource(storageService.loadAsResource(document.getStorageUrl()))
                .build();
    }

    // Lay user tu JWT.
    private User getCurrentUser(JwtAuthenticationToken authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        return userRepo.findById(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    // Lay group theo id.
    private StudyGroup getGroup(String groupId) {
        return groupRepo.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
    }

    // Lay membership trong group.
    private GroupMember getMembership(StudyGroup group, User user) {
        return memberRepo.findByGroupAndUser(group, user)
                .orElseThrow(() -> new AppException(ErrorCode.MEMBER_NOT_FOUND_IN_GROUP));
    }

    // OWNER luon duoc upload, MEMBER can leader cap quyen.
    private void checkUploadPermission(GroupMember membership) {
        if (membership.getRole() == GroupRole.OWNER) {
            return;
        }

        if (!Boolean.TRUE.equals(membership.getCanUploadDocument())) {
            throw new AppException(ErrorCode.GROUP_DOCUMENT_UPLOAD_NOT_ALLOWED);
        }
    }

    // Lay document thuoc dung group.
    private Document getGroupDocument(StudyGroup group, String documentId) {
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCUMENT_NOT_FOUND));

        if (document.getGroup() == null
                || !group.getId().equals(document.getGroup().getId())
                || Boolean.TRUE.equals(document.getDeleted())) {
            throw new AppException(ErrorCode.DOCUMENT_NOT_FOUND);
        }

        return document;
    }

    // Kiem tra dung luong cua user.
    private void validateStorageCapacity(User owner, long newFileSize) {
        Long usedStorageResult = documentRepo.sumFileSizeByOwner(owner);
        long usedStorage = usedStorageResult == null ? 0 : usedStorageResult;

        if (usedStorage + newFileSize > maxUserStorage) {
            throw new AppException(ErrorCode.STORAGE_NOT_ENOUGH);
        }
    }

    // Lay ten file goc an toan.
    private String cleanOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new AppException(ErrorCode.FILE_TYPE_NOT_SUPPORTED);
        }

        String cleanPath = StringUtils.cleanPath(originalFileName);
        int slashIndex = Math.max(cleanPath.lastIndexOf('/'), cleanPath.lastIndexOf('\\'));

        if (slashIndex >= 0) {
            return cleanPath.substring(slashIndex + 1);
        }

        return cleanPath;
    }

    // Tao duong dan luu file theo group.
    private String buildGroupStoredFileName(String groupId, String originalFileName) {
        return "groups/" + groupId + "/documents/" + UUID.randomUUID() + "-" + originalFileName;
    }

    // Xoa file neu luu DB loi.
    private void deleteStorageQuietly(String storageUrl) {
        try {
            storageService.delete(storageUrl);
        } catch (RuntimeException ignored) {
        }
    }

    // Tao response tra ve FE.
    private DocumentResponse buildDocumentResponse(Document document) {
        StudyGroup group = document.getGroup();
        User owner = document.getOwner();

        return DocumentResponse.builder()
                .documentId(document.getId())
                .title(document.getTitle())
                .fileName(document.getFileName())
                .fileExtension(document.getFileExtension())
                .mimeType(document.getMimeType())
                .fileSize(document.getFileSize())
                .storageUrl(document.getStorageUrl())
                .status(document.getStatus().name())
                .ownerId(owner == null ? null : owner.getId())
                .ownerFullname(owner == null ? null : owner.getFullname())
                .groupId(group == null ? null : group.getId())
                .groupName(group == null ? null : group.getName())
                .deleted(Boolean.TRUE.equals(document.getDeleted()))
                .deletedAt(document.getDeletedAt() == null ? null : document.getDeletedAt().toString())
                .build();
    }

    // Leader xoa file trong group vao thung rac.
    @Transactional
    public DeleteDocumentResponse deleteGroupDocument(
            String groupId,
            String documentId,
            JwtAuthenticationToken authentication
    ) {
        User leader = getCurrentUser(authentication);
        StudyGroup group = getGroup(groupId);

        GroupMember membership = getMembership(group, leader);
        checkLeaderPermission(membership);

        Document document = getGroupDocument(group, documentId);

        document.setDeleted(true);
        document.setDeletedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        documentRepo.save(document);

        return DeleteDocumentResponse.builder()
                .documentId(documentId)
                .deleted(true)
                .build();
    }
    // Chi OWNER duoc quan ly file group.
    private void checkLeaderPermission(GroupMember membership) {
        if (membership.getRole() != GroupRole.OWNER) {
            throw new AppException(ErrorCode.UNAUTHORIZED_GROUP_ACTION);
        }
    }
}
