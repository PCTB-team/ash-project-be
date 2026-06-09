package com.pctb.webapp.service;

import com.pctb.webapp.dto.response.GroupFileResponse;
import com.pctb.webapp.entity.GroupFile;
import com.pctb.webapp.entity.GroupMember;
import com.pctb.webapp.entity.GroupRole;
import com.pctb.webapp.entity.JoinStatus;
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
     * Upload file vao group.
     * User phai la member APPROVED va canUpload=true moi duoc upload.
     */
    @Transactional
    public GroupFileResponse uploadFile(
            String groupId,
            MultipartFile file,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        GroupMember membership = requireUploadPermission(groupId, currentUser);
        StudyGroup group = membership.getGroup();

        String realMimeType = fileValidationService.validate(file);
        String originalFileName = cleanOriginalFileName(file.getOriginalFilename());
        String extension = fileValidationService.getExtension(originalFileName);
        String storageFileName = buildStoredFileName(group.getId(), extension);
        String storageUrl = storageService.upload(file, storageFileName);

        GroupFile groupFile = GroupFile.builder()
                .fileName(originalFileName)
                .fileExtension(extension)
                .mimeType(realMimeType)
                .fileSize(file.getSize())
                .storageUrl(storageUrl)
                .group(group)
                .uploadedBy(currentUser)
                .uploadedAt(LocalDateTime.now())
                .build();

        return buildGroupFileResponse(groupFileRepo.save(groupFile));
    }

    /**
     * Lay danh sach file trong group.
     * Chi member APPROVED moi duoc xem file cua group.
     */
    @Transactional
    public List<GroupFileResponse> getGroupFiles(
            String groupId,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        requireApprovedMember(groupId, currentUser);

        return groupFileRepo.findByGroupIdOrderByUploadedAtDesc(groupId)
                .stream()
                .map(this::buildGroupFileResponse)
                .toList();
    }

    /**
     * Lay current user tu JWT.
     * authentication.getName() la userId trong project nay.
     */
    private User getCurrentUser(JwtAuthenticationToken authentication) {
        return userRepo.findById(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Check user da duoc duyet vao group va co quyen upload file.
     */
    private GroupMember requireUploadPermission(String groupId, User currentUser) {
        GroupMember member = requireApprovedMember(groupId, currentUser);
        if (!Boolean.TRUE.equals(member.getCanUpload())) {
            throw new AppException(ErrorCode.GROUP_UPLOAD_NOT_ALLOWED);
        }

        return member;
    }

    /**
     * Check user la member APPROVED cua group.
     * PENDING/REJECTED/LEFT/BANNED deu khong duoc xem/upload file group.
     */
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
                && (member.getRole() != GroupRole.LEADER || member.getJoinStatus() != JoinStatus.APPROVED)) {
            return syncOwnerLeaderMembership(member);
        }

        if (member.getJoinStatus() != JoinStatus.APPROVED) {
            throw new AppException(ErrorCode.GROUP_MEMBER_NOT_APPROVED);
        }

        return member;
    }

    /**
     * Tao membership LEADER cho owner neu group cu chua co record membership.
     */
    private GroupMember createOwnerLeaderMembership(StudyGroup group, User owner) {
        GroupMember member = GroupMember.builder()
                .group(group)
                .user(owner)
                .role(GroupRole.LEADER)
                .joinStatus(JoinStatus.APPROVED)
                .canUpload(true)
                .joinedAt(LocalDateTime.now())
                .build();

        return groupMemberRepo.save(member);
    }

    /**
     * Dong bo membership cua owner thanh LEADER APPROVED de khong bi mat quyen voi data cu.
     */
    private GroupMember syncOwnerLeaderMembership(GroupMember member) {
        member.setRole(GroupRole.LEADER);
        member.setJoinStatus(JoinStatus.APPROVED);
        member.setCanUpload(true);
        if (member.getJoinedAt() == null) {
            member.setJoinedAt(LocalDateTime.now());
        }

        return groupMemberRepo.save(member);
    }

    /**
     * Lam sach ten file goc de tranh client gui kem path.
     */
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

    /**
     * Tao path luu file group trong local storage.
     */
    private String buildStoredFileName(String groupId, String extension) {
        return "groups/" + groupId + "/files/" + UUID.randomUUID() + "." + extension;
    }

    /**
     * Convert GroupFile entity sang DTO tra ve FE.
     */
    private GroupFileResponse buildGroupFileResponse(GroupFile groupFile) {
        return GroupFileResponse.builder()
                .fileId(groupFile.getId())
                .fileName(groupFile.getFileName())
                .mimeType(groupFile.getMimeType())
                .fileSize(groupFile.getFileSize())
                .storageUrl(groupFile.getStorageUrl())
                .uploadedBy(groupFile.getUploadedBy().getFullname())
                .uploadedAt(groupFile.getUploadedAt() == null ? null : groupFile.getUploadedAt().toString())
                .build();
    }

    /**
     * Chuan hoa id bat buoc trong path variable.
     */
    private String normalizeRequiredText(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(ErrorCode.REQUEST_PARAMETER_INVALID);
        }

        return value.trim();
    }
}
