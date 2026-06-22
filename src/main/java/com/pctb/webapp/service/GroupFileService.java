package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.SaveGroupFileToDashboardRequest;
import com.pctb.webapp.dto.response.DeleteGroupFileResponse;
import com.pctb.webapp.dto.response.DocumentResponse;
import com.pctb.webapp.dto.response.GroupFileResponse;
import com.pctb.webapp.entity.Document;
import com.pctb.webapp.entity.Folder;
import com.pctb.webapp.entity.GroupFile;
import com.pctb.webapp.entity.GroupMember;
import com.pctb.webapp.entity.GroupRole;
import com.pctb.webapp.entity.NotificationType;
import com.pctb.webapp.entity.StudyGroup;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.exception.UploadStatus;
import com.pctb.webapp.repository.DocumentRepo;
import com.pctb.webapp.repository.FolderRepo;
import com.pctb.webapp.repository.GroupFileRepo;
import com.pctb.webapp.repository.GroupMemberRepo;
import com.pctb.webapp.repository.StudyGroupRepo;
import com.pctb.webapp.repository.UserRepo;
import com.pctb.webapp.util.DateTimeUtils;
import com.pctb.webapp.util.DocumentPreviewUtils;
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
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupFileService {
    GroupFileRepo groupFileRepo;

    GroupMemberRepo groupMemberRepo;

    StudyGroupRepo studyGroupRepo;

    UserRepo userRepo;

    DocumentRepo documentRepo;

    FolderRepo folderRepo;

    FileValidationService fileValidationService;

    StorageService storageService;

    NotificationService notificationService;

    @Value("${app.upload.max-user-storage}")
    @NonFinal
    long maxUserStorage;

    /**
     * Upload file into a group.
     * User must be a group member and have canUpload=true.
     */
    @Transactional
    public GroupFileResponse uploadFile(
            String groupId,
            MultipartFile file,
            Boolean replaceExisting,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        GroupMember membership = requireUploadPermission(groupId, currentUser);
        StudyGroup group = membership.getGroup();

        String realMimeType = fileValidationService.validate(file);
        String originalFileName = cleanOriginalFileName(file.getOriginalFilename());
        String extension = fileValidationService.getExtension(originalFileName);
        String storageFileName = buildStoredFileName(group.getId(), extension);
        boolean shouldReplace = Boolean.TRUE.equals(replaceExisting);
        GroupFile existingFile = groupFileRepo.findByGroupIdAndFileNameAndDeletedFalse(group.getId(), originalFileName)
                .orElse(null);

        if (existingFile != null && !shouldReplace) {
            throw new AppException(ErrorCode.FILE_ALREADY_EXISTS);
        }

        String storageUrl = storageService.upload(file, storageFileName);
        LocalDateTime now = LocalDateTime.now();

        GroupFile groupFile;
        if (existingFile == null) {
            groupFile = GroupFile.builder()
                    .fileName(originalFileName)
                    .fileExtension(extension)
                    .mimeType(realMimeType)
                    .fileSize(file.getSize())
                    .storageUrl(storageUrl)
                    .group(group)
                    .uploadedBy(currentUser)
                    .uploadedAt(now)
                    .deleted(false)
                    .build();
        } else {
            storageService.delete(existingFile.getStorageUrl());
            existingFile.setFileName(originalFileName);
            existingFile.setFileExtension(extension);
            existingFile.setMimeType(realMimeType);
            existingFile.setFileSize(file.getSize());
            existingFile.setStorageUrl(storageUrl);
            existingFile.setUploadedBy(currentUser);
            existingFile.setUploadedAt(now);
            existingFile.setDeleted(false);
            existingFile.setDeletedAt(null);
            groupFile = existingFile;
        }

        groupFile = groupFileRepo.save(groupFile);

        // Bao cho cac thanh vien khac biet co file moi trong group.
        notificationService.createForGroupMembers(
                group,
                currentUser,
                NotificationType.GROUP_FILE_UPLOADED,
                "Tai lieu moi trong nhom",
                currentUser.getFullname() + " da tai len \"" + groupFile.getFileName()
                        + "\" trong nhom \"" + group.getName() + "\".",
                NotificationType.GROUP_FILE,
                groupFile.getId(),
                groupFile.getFileName(),
                Set.of(currentUser.getId())
        );

        return buildGroupFileResponse(groupFile);
    }

    /**
     * Get active or trashed files in a group.
     * Active files are visible to group members; trash files are visible to leader only.
     */
    @Transactional
    public List<GroupFileResponse> getGroupFiles(
            String groupId,
            Boolean deleted,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        if (Boolean.TRUE.equals(deleted)) {
            requireLeader(groupId, currentUser);

            return groupFileRepo.findByGroupIdAndDeletedTrueOrderByDeletedAtDesc(groupId)
                    .stream()
                    .map(this::buildGroupFileResponse)
                    .toList();
        }

        requireApprovedMember(groupId, currentUser);

        return groupFileRepo.findByGroupIdAndDeletedFalseOrderByUploadedAtDesc(groupId)
                .stream()
                .map(this::buildGroupFileResponse)
                .toList();
    }

    /**
     * Save a group file into current user's dashboard.
     */
    @Transactional
    public DocumentResponse saveFileToDashboard(
            String groupId,
            String fileId,
            SaveGroupFileToDashboardRequest request,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = userRepo.findByIdForUpdate(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        requireApprovedMember(groupId, currentUser);

        GroupFile groupFile = getFileInGroup(groupId, fileId);
        if (Boolean.TRUE.equals(groupFile.getDeleted())) {
            throw new AppException(ErrorCode.GROUP_FILE_ALREADY_DELETED);
        }

        String folderId = normalizeOptionalId(request == null ? null : request.getFolderId());
        Folder folder = resolveFolder(folderId, currentUser.getId());
        boolean shouldReplace = request != null && Boolean.TRUE.equals(request.getReplaceExisting());
        Document existingDocument = documentRepo
                .findByOwnerIdAndFolderIdAndFileName(currentUser.getId(), folderId, groupFile.getFileName())
                .orElse(null);

        if (existingDocument != null && !shouldReplace) {
            throw new AppException(ErrorCode.FILE_ALREADY_EXISTS);
        }

        validateStorageCapacity(currentUser, existingDocument, safeFileSize(groupFile));

        if (existingDocument != null) {
            storageService.delete(existingDocument.getStorageUrl());
            if (!Boolean.TRUE.equals(existingDocument.getDeleted())) {
                updateFolderSizeCascade(existingDocument.getFolder(), -safeFileSize(existingDocument));
            }
            documentRepo.delete(existingDocument);
        }

        String extension = normalizeFileExtension(groupFile);
        String storageFileName = buildDocumentStoredFileName(currentUser.getId(), extension);
        String storageUrl = storageService.copy(groupFile.getStorageUrl(), storageFileName);
        LocalDateTime now = DateTimeUtils.nowUtc();

        Document document = Document.builder()
                .title(groupFile.getFileName())
                .fileName(groupFile.getFileName())
                .fileExtension(extension)
                .mimeType(groupFile.getMimeType())
                .fileSize(safeFileSize(groupFile))
                .storageUrl(storageUrl)
                .status(UploadStatus.COMPLETED)
                .owner(currentUser)
                .folder(folder)
                .createdAt(now)
                .updatedAt(now)
                .deleted(false)
                .build();

        Document savedDocument = documentRepo.save(document);
        updateFolderSizeCascade(folder, safeFileSize(savedDocument));

        return buildDocumentResponse(savedDocument);
    }

    /**
     * Move a group file to trash.
     */
    @Transactional
    public void moveFileToTrash(
            String groupId,
            String fileId,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        requireLeader(groupId, currentUser);

        GroupFile groupFile = getFileInGroup(groupId, fileId);
        if (Boolean.TRUE.equals(groupFile.getDeleted())) {
            throw new AppException(ErrorCode.GROUP_FILE_ALREADY_DELETED);
        }

        groupFile.setDeleted(true);
        groupFile.setDeletedAt(LocalDateTime.now());
        groupFileRepo.save(groupFile);

        // Bao cho cac thanh vien khac biet file da duoc dua vao thung rac.
        notificationService.createForGroupMembers(
                groupFile.getGroup(),
                currentUser,
                NotificationType.GROUP_FILE_MOVED_TO_TRASH,
                "Tai lieu da bi xoa",
                "\"" + groupFile.getFileName() + "\" da duoc chuyen vao thung rac trong nhom \""
                        + groupFile.getGroup().getName() + "\".",
                NotificationType.GROUP_FILE,
                groupFile.getId(),
                groupFile.getFileName(),
                Set.of(currentUser.getId())
        );
    }

    /**
     * Permanently delete a group file from trash.
     */
    @Transactional
    public DeleteGroupFileResponse deleteFilePermanently(
            String groupId,
            String fileId,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        requireLeader(groupId, currentUser);

        GroupFile groupFile = getFileInGroup(groupId, fileId);
        if (!Boolean.TRUE.equals(groupFile.getDeleted())) {
            throw new AppException(ErrorCode.GROUP_FILE_NOT_DELETED);
        }

        String deletedFileId = groupFile.getId();
        String deletedFileName = groupFile.getFileName();
        storageService.delete(groupFile.getStorageUrl());
        groupFileRepo.delete(groupFile);

        return DeleteGroupFileResponse.builder()
                .fileId(deletedFileId)
                .fileName(deletedFileName)
                .deletedPermanently(true)
                .build();
    }

    /**
     * Restore a group file from trash.
     */
    @Transactional
    public GroupFileResponse restoreFile(
            String groupId,
            String fileId,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        requireLeader(groupId, currentUser);

        GroupFile groupFile = getFileInGroup(groupId, fileId);
        if (!Boolean.TRUE.equals(groupFile.getDeleted())) {
            throw new AppException(ErrorCode.GROUP_FILE_NOT_DELETED);
        }

        groupFile.setDeleted(false);
        groupFile.setDeletedAt(null);

        GroupFile savedFile = groupFileRepo.save(groupFile);
        notificationService.createForGroupMembers(
                savedFile.getGroup(),
                currentUser,
                NotificationType.GROUP_FILE_RESTORED,
                "Tai lieu da duoc khoi phuc",
                "\"" + savedFile.getFileName() + "\" da duoc khoi phuc trong nhom \""
                        + savedFile.getGroup().getName() + "\".",
                NotificationType.GROUP_FILE,
                savedFile.getId(),
                savedFile.getFileName(),
                Set.of(currentUser.getId())
        );

        return buildGroupFileResponse(savedFile);
    }

    private User getCurrentUser(JwtAuthenticationToken authentication) {
        return userRepo.findById(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private GroupMember requireUploadPermission(String groupId, User currentUser) {
        GroupMember member = requireApprovedMember(groupId, currentUser);
        if (!Boolean.TRUE.equals(member.getCanUpload())) {
            throw new AppException(ErrorCode.GROUP_UPLOAD_NOT_ALLOWED);
        }

        return member;
    }

    private GroupMember requireApprovedMember(String groupId, User currentUser) {
        StudyGroup group = studyGroupRepo.findById(normalizeRequiredText(groupId))
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        GroupMember member = groupMemberRepo.findByGroupAndUser(group, currentUser)
                .orElse(null);

        if (member == null && group.getOwner().getId().equals(currentUser.getId())) {
            return createOwnerLeaderMembership(group, currentUser);
        }

        if (member == null) {
            throw new AppException(ErrorCode.GROUP_ACCESS_DENIED);
        }

        if (group.getOwner().getId().equals(currentUser.getId())
                && member.getRole() != GroupRole.LEADER) {
            return syncOwnerLeaderMembership(member);
        }

        return member;
    }

    private GroupMember requireLeader(String groupId, User currentUser) {
        GroupMember member = requireApprovedMember(groupId, currentUser);
        if (member.getRole() != GroupRole.LEADER) {
            throw new AppException(ErrorCode.GROUP_ACCESS_DENIED);
        }

        return member;
    }

    private GroupFile getFileInGroup(String groupId, String fileId) {
        GroupFile groupFile = groupFileRepo.findById(normalizeRequiredText(fileId))
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_FILE_NOT_FOUND));

        if (!groupFile.getGroup().getId().equals(normalizeRequiredText(groupId))) {
            throw new AppException(ErrorCode.GROUP_FILE_NOT_IN_GROUP);
        }

        return groupFile;
    }

    private Folder resolveFolder(String folderId, String userId) {
        if (folderId == null) {
            return null;
        }

        return folderRepo.findActiveByIdAndOwnerId(folderId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.FOLDER_NOT_FOUND));
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

    private void updateFolderSizeCascade(Folder folder, long delta) {
        Folder current = folder;

        while (current != null) {
            long currentSize = current.getSize() == null ? 0 : current.getSize();
            current.setSize(Math.max(0, currentSize + delta));
            current.setUpdatedAt(DateTimeUtils.nowUtc());
            folderRepo.save(current);
            current = current.getParent();
        }
    }

    private long safeFileSize(Document document) {
        return document.getFileSize() == null ? 0 : document.getFileSize();
    }

    private long safeFileSize(GroupFile groupFile) {
        return groupFile.getFileSize() == null ? 0 : groupFile.getFileSize();
    }

    private GroupMember createOwnerLeaderMembership(StudyGroup group, User owner) {
        GroupMember member = GroupMember.builder()
                .group(group)
                .user(owner)
                .role(GroupRole.LEADER)
                .canUpload(true)
                .joinedAt(LocalDateTime.now())
                .build();

        return groupMemberRepo.save(member);
    }

    private GroupMember syncOwnerLeaderMembership(GroupMember member) {
        member.setRole(GroupRole.LEADER);
        member.setCanUpload(true);
        if (member.getJoinedAt() == null) {
            member.setJoinedAt(LocalDateTime.now());
        }

        return groupMemberRepo.save(member);
    }

    private String cleanOriginalFileName(String originalFileName) {
        String cleanPath = StringUtils.cleanPath(originalFileName == null ? "" : originalFileName);
        int slashIndex = Math.max(cleanPath.lastIndexOf('/'), cleanPath.lastIndexOf('\\'));

        if (slashIndex >= 0) {
            cleanPath = cleanPath.substring(slashIndex + 1);
        }

        if (cleanPath.isBlank()) {
            throw new AppException(ErrorCode.DOCUMENT_FILE_NAME_REQUIRED);
        }

        return cleanPath;
    }

    private String buildStoredFileName(String groupId, String extension) {
        return "groups/" + groupId + "/files/" + UUID.randomUUID() + "." + extension;
    }

    private String buildDocumentStoredFileName(String userId, String extension) {
        return "documents/" + userId + "/" + UUID.randomUUID() + "." + extension;
    }

    private GroupFileResponse buildGroupFileResponse(GroupFile groupFile) {
        String fileExtension = extractFileExtension(groupFile.getFileName());
        return GroupFileResponse.builder()
                .fileId(groupFile.getId())
                .fileName(groupFile.getFileName())
                .mimeType(groupFile.getMimeType())
                .fileSize(groupFile.getFileSize())
                .storageUrl(groupFile.getStorageUrl())
                .previewUrl(DocumentPreviewUtils.resolvePreviewUrl(fileExtension, groupFile.getStorageUrl()))
                .previewMode(DocumentPreviewUtils.resolvePreviewMode(fileExtension))
                .previewSupported(DocumentPreviewUtils.isPreviewSupported(fileExtension))
                .uploadedBy(groupFile.getUploadedBy().getFullname())
                .uploadedAt(groupFile.getUploadedAt() == null ? null : groupFile.getUploadedAt().toString())
                .deleted(Boolean.TRUE.equals(groupFile.getDeleted()))
                .deletedAt(groupFile.getDeletedAt() == null ? null : groupFile.getDeletedAt().toString())
                .build();
    }

    private DocumentResponse buildDocumentResponse(Document document) {
        User owner = document.getOwner();

        return DocumentResponse.builder()
                .documentId(document.getId())
                .title(document.getTitle())
                .fileName(document.getFileName())
                .fileExtension(document.getFileExtension())
                .mimeType(document.getMimeType())
                .fileSize(document.getFileSize())
                .storageUrl(document.getStorageUrl())
                .folderId(document.getFolder() == null ? null : document.getFolder().getId())
                .viewUrl("/documents/" + document.getId() + "/view")
                .downloadUrl("/documents/" + document.getId() + "/download")
                .status(document.getStatus().name())
                .ownerId(owner.getId())
                .ownerFullname(owner.getFullname())
                .uploadedAt(DateTimeUtils.toDisplayDateTime(document.getCreatedAt()))
                .timeSinceUpload(DateTimeUtils.formatTimeSince(document.getCreatedAt()))
                .deleted(Boolean.TRUE.equals(document.getDeleted()))
                .deletedAt(DateTimeUtils.toDisplayDateTime(document.getDeletedAt()))
                .build();
    }

    private String normalizeFileExtension(GroupFile groupFile) {
        if (groupFile.getFileExtension() != null && !groupFile.getFileExtension().isBlank()) {
            return groupFile.getFileExtension();
        }

        String extension = extractFileExtension(groupFile.getFileName());
        if (extension.isBlank()) {
            throw new AppException(ErrorCode.DOCUMENT_FILE_NAME_REQUIRED);
        }

        return extension;
    }

    private String extractFileExtension(String fileName) {
        if (fileName == null) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex < 0 || lastDotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(lastDotIndex + 1);
    }

    private String normalizeRequiredText(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorCode.REQUEST_PARAMETER_INVALID);
        }

        return value.trim();
    }

    private String normalizeOptionalId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
