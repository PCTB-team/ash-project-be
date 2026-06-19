package com.pctb.webapp.service;

import com.pctb.webapp.dto.response.GroupFileResponse;
import com.pctb.webapp.entity.GroupFile;
import com.pctb.webapp.entity.GroupMember;
import com.pctb.webapp.entity.GroupRole;
import com.pctb.webapp.entity.StudyGroup;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.GroupFileRepo;
import com.pctb.webapp.repository.GroupMemberRepo;
import com.pctb.webapp.repository.StudyGroupRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
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
public class GroupFileService {
    GroupFileRepo groupFileRepo;

    GroupMemberRepo groupMemberRepo;

    StudyGroupRepo studyGroupRepo;

    UserRepo userRepo;

    FileValidationService fileValidationService;

    StorageService storageService;

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

        return buildGroupFileResponse(groupFileRepo.save(groupFile));
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

    private GroupFileResponse buildGroupFileResponse(GroupFile groupFile) {
        return GroupFileResponse.builder()
                .fileId(groupFile.getId())
                .fileName(groupFile.getFileName())
                .mimeType(groupFile.getMimeType())
                .fileSize(groupFile.getFileSize())
                .storageUrl(groupFile.getStorageUrl())
                .uploadedBy(groupFile.getUploadedBy().getFullname())
                .uploadedAt(groupFile.getUploadedAt() == null ? null : groupFile.getUploadedAt().toString())
                .deleted(Boolean.TRUE.equals(groupFile.getDeleted()))
                .deletedAt(groupFile.getDeletedAt() == null ? null : groupFile.getDeletedAt().toString())
                .build();
    }

    private String normalizeRequiredText(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorCode.REQUEST_PARAMETER_INVALID);
        }

        return value.trim();
    }
}
