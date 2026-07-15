package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.CreateGroupRequest;
import com.pctb.webapp.dto.request.JoinGroupRequest;
import com.pctb.webapp.dto.request.UpdateChatPermissionRequest;
import com.pctb.webapp.dto.request.UpdateGroupPasswordRequest;
import com.pctb.webapp.dto.request.UpdateUploadPermissionRequest;
import com.pctb.webapp.dto.response.CreateGroupResponse;
import com.pctb.webapp.dto.response.DeleteGroupResponse;
import com.pctb.webapp.dto.response.GroupMemberResponse;
import com.pctb.webapp.dto.response.GroupMembersResponse;
import com.pctb.webapp.dto.response.GroupPageResponse;
import com.pctb.webapp.dto.response.GroupPreviewResponse;
import com.pctb.webapp.dto.response.GroupSummaryResponse;
import com.pctb.webapp.entity.GroupMember;
import com.pctb.webapp.entity.GroupFile;
import com.pctb.webapp.entity.GroupRole;
import com.pctb.webapp.entity.NotificationType;
import com.pctb.webapp.entity.StudyGroup;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.event.GroupDeletedEvent;
import com.pctb.webapp.repository.GroupFileRepo;
import com.pctb.webapp.repository.GroupMemberRepo;
import com.pctb.webapp.repository.GroupMessageRepo;
import com.pctb.webapp.repository.StudyGroupRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupService {
    StudyGroupRepo studyGroupRepo;

    GroupMemberRepo groupMemberRepo;

    GroupFileRepo groupFileRepo;

    GroupMessageRepo groupMessageRepo;

    UserRepo userRepo;

    PasswordEncoder passwordEncoder;

    NotificationService notificationService;

    ApplicationEventPublisher eventPublisher;

    @Value("${app.group.invite-base-url:http://localhost:3000/join}")
    @NonFinal
    String inviteBaseUrl;

    @Transactional
    public CreateGroupResponse createGroup(
            CreateGroupRequest request,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        String name = normalizeRequiredText(request == null ? null : request.getName());
        String password = normalizeRequiredText(request == null ? null : request.getPassword());
        String description = normalizeOptionalText(request == null ? null : request.getDescription());
        LocalDateTime now = LocalDateTime.now();

        StudyGroup group = StudyGroup.builder()
                .name(name)
                .description(description)
                .inviteToken(generateInviteToken())
                .password(passwordEncoder.encode(password))
                .owner(currentUser)
                .inviteEnabled(true)
                .createdAt(now)
                .updatedAt(now)
                .build();

        StudyGroup savedGroup = studyGroupRepo.save(group);

        GroupMember leaderMember = GroupMember.builder()
                .group(savedGroup)
                .user(currentUser)
                .role(GroupRole.LEADER)
                .canUpload(true)
                .canChat(true)
                .joinedAt(now)
                .build();
        groupMemberRepo.save(leaderMember);

        return buildCreateGroupResponse(savedGroup);
    }

    /**
     * Lay cac group ma user dang dang nhap dang tham gia hoac dang lam leader.
     */
    @Transactional(readOnly = true)
    public GroupPageResponse getMyGroups(
            String keyword,
            int page,
            int size,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        Pageable pageable = PageRequest.of(normalizePage(page), normalizePageSize(size));
        String normalizedKeyword = normalizeOptionalText(keyword);

        // Query co phan trang de man danh sach group chi load dung so luong FE can hien thi.
        Page<StudyGroup> groupPage = studyGroupRepo.findMyGroups(
                currentUser.getId(),
                normalizedKeyword,
                pageable
        );

        List<GroupSummaryResponse> groups = groupPage.getContent().stream()
                .map(group -> {
                    GroupMember member = groupMemberRepo
                            .findByGroupIdAndUserId(group.getId(), currentUser.getId())
                            .orElse(null);

                    return buildGroupSummaryResponse(group, member, currentUser);
                })
                .toList();

        return GroupPageResponse.builder()
                .items(groups)
                .page(groupPage.getNumber())
                .size(groupPage.getSize())
                .totalElements(groupPage.getTotalElements())
                .totalPages(groupPage.getTotalPages())
                .build();
    }

    @Transactional
    public GroupSummaryResponse getGroupDetail(
            String groupId,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        GroupMember member = requireApprovedMember(groupId, currentUser);

        // Tra ve thong tin group kem role/quyen cua user hien tai de FE render trang detail.
        return buildGroupSummaryResponse(member.getGroup(), member, currentUser);
    }

    @Transactional(readOnly = true)
    public GroupPreviewResponse getGroupPreview(String inviteToken) {
        StudyGroup group = getGroupByInviteToken(inviteToken);
        ensureInviteEnabled(group);

        return GroupPreviewResponse.builder()
                .groupName(group.getName())
                .description(group.getDescription())
                .ownerName(group.getOwner().getFullname())
                .build();
    }

    @Transactional
    public void joinByInvite(
            String inviteToken,
            JoinGroupRequest request,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        StudyGroup group = getGroupByInviteToken(inviteToken);
        ensureInviteEnabled(group);

        String password = normalizeRequiredText(request == null ? null : request.getPassword());
        if (!passwordEncoder.matches(password, group.getPassword())) {
            throw new AppException(ErrorCode.GROUP_PASSWORD_INCORRECT);
        }

        groupMemberRepo.findByGroupAndUser(group, currentUser)
                .ifPresent(existingMember -> {
                    throw new AppException(ErrorCode.USER_ALREADY_IN_GROUP);
                });

        GroupMember member = GroupMember.builder()
                .group(group)
                .user(currentUser)
                .role(GroupRole.MEMBER)
                .canUpload(false)
                .canChat(true)
                .joinedAt(LocalDateTime.now())
                .build();
        groupMemberRepo.save(member);

        // Thong bao cho cac thanh vien cu biet co user moi tham gia group.
        notificationService.createForGroupMembers(
                group,
                currentUser,
                NotificationType.GROUP_MEMBER_JOINED,
                "New member joined",
                currentUser.getFullname() + " joined group \"" + group.getName() + "\".",
                NotificationType.GROUP,
                group.getId(),
                group.getName(),
                Set.of(currentUser.getId())
        );
    }

    @Transactional
    public GroupMemberResponse updateUploadPermission(
            String groupId,
            String memberId,
            UpdateUploadPermissionRequest request,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        requireLeader(groupId, currentUser);

        if (request == null || request.getCanUpload() == null) {
            throw new AppException(ErrorCode.REQUEST_BODY_INVALID);
        }

        GroupMember member = getMemberInGroup(groupId, memberId);
        member.setCanUpload(request.getCanUpload());
        GroupMember savedMember = groupMemberRepo.save(member);
        StudyGroup group = savedMember.getGroup();
        User targetUser = savedMember.getUser();

        // Bao rieng cho member duoc cap/thu hoi quyen upload.
        notificationService.create(
                targetUser,
                currentUser,
                Boolean.TRUE.equals(request.getCanUpload())
                        ? NotificationType.GROUP_UPLOAD_PERMISSION_GRANTED
                        : NotificationType.GROUP_UPLOAD_PERMISSION_REVOKED,
                Boolean.TRUE.equals(request.getCanUpload())
                        ? "You have been granted upload permission"
                        : "Upload permission has been revoked",
                Boolean.TRUE.equals(request.getCanUpload())
                        ? "The leader allowed you to upload documents to group \"" + group.getName() + "\"."
                        : "You no longer have permission to upload documents to group \"" + group.getName() + "\".",
                NotificationType.GROUP,
                group.getId(),
                group.getName(),
                group.getId(),
                group.getName()
        );

        // Bao cho cac member con lai de ho thay activity cua group.
        notificationService.createForGroupMembers(
                group,
                currentUser,
                Boolean.TRUE.equals(request.getCanUpload())
                        ? NotificationType.GROUP_UPLOAD_PERMISSION_GRANTED
                        : NotificationType.GROUP_UPLOAD_PERMISSION_REVOKED,
                Boolean.TRUE.equals(request.getCanUpload())
                        ? "Upload permission granted"
                        : "Upload permission has been revoked",
                Boolean.TRUE.equals(request.getCanUpload())
                        ? targetUser.getFullname() + " was granted upload permission in group \"" + group.getName() + "\"."
                        : targetUser.getFullname() + " had upload permission revoked in group \"" + group.getName() + "\".",
                NotificationType.GROUP,
                group.getId(),
                group.getName(),
                Set.of(currentUser.getId(), targetUser.getId())
        );

        return buildGroupMemberResponse(savedMember);
    }

    @Transactional
    public GroupMemberResponse updateChatPermission(
            String groupId,
            String memberId,
            UpdateChatPermissionRequest request,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        requireLeaderForPermission(groupId, currentUser);

        if (request == null || request.getCanChat() == null) {
            throw new AppException(ErrorCode.REQUEST_BODY_INVALID);
        }

        GroupMember member = getMemberInGroup(groupId, memberId);
        if (member.getRole() == GroupRole.LEADER) {
            throw new AppException(ErrorCode.GROUP_LEADER_CANNOT_BE_MUTED);
        }

        member.setCanChat(request.getCanChat());
        return buildGroupMemberResponse(groupMemberRepo.save(member));
    }

    @Transactional
    public GroupMemberResponse kickMember(
            String groupId,
            String memberId,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        requireLeader(groupId, currentUser);

        GroupMember member = getMemberInGroup(groupId, memberId);
        if (member.getRole() == GroupRole.LEADER) {
            throw new AppException(ErrorCode.GROUP_LEADER_CANNOT_BE_KICKED);
        }

        StudyGroup group = member.getGroup();
        User kickedUser = member.getUser();
        notificationService.create(
                kickedUser,
                currentUser,
                NotificationType.GROUP_MEMBER_KICKED,
                "You have been removed from the group",
                "The leader removed you from group \"" + group.getName() + "\".",
                NotificationType.GROUP,
                group.getId(),
                group.getName(),
                group.getId(),
                group.getName()
        );

        // Bao cho cac member con lai biet user nay da bi kick khoi group.
        notificationService.createForGroupMembers(
                group,
                currentUser,
                NotificationType.GROUP_MEMBER_KICKED,
                "Member removed from the group",
                kickedUser.getFullname() + " was removed from group \"" + group.getName() + "\".",
                NotificationType.GROUP,
                group.getId(),
                group.getName(),
                Set.of(currentUser.getId(), kickedUser.getId())
        );

        member.setCanUpload(false);
        GroupMemberResponse response = buildGroupMemberResponse(member);
        groupMemberRepo.delete(member);

        return response;
    }

    @Transactional
    public GroupMemberResponse leaveGroup(
            String groupId,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        GroupMember member = groupMemberRepo
                .findByGroupIdAndUserId(normalizeRequiredText(groupId), currentUser.getId())
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_ACCESS_DENIED));

        // Leader khong duoc roi group bang luong member; can chuyen quyen hoac xoa group rieng.
        if (member.getRole() == GroupRole.LEADER) {
            throw new AppException(ErrorCode.GROUP_LEADER_CANNOT_LEAVE);
        }

        member.setCanUpload(false);
        StudyGroup group = member.getGroup();

        // Bao cho cac thanh vien con lai biet user nay da roi group.
        notificationService.createForGroupMembers(
                group,
                currentUser,
                NotificationType.GROUP_MEMBER_LEFT,
                "Member left the group",
                currentUser.getFullname() + " left group \"" + group.getName() + "\".",
                NotificationType.GROUP,
                group.getId(),
                group.getName(),
                Set.of(currentUser.getId())
        );

        GroupMemberResponse response = buildGroupMemberResponse(member);
        groupMemberRepo.delete(member);

        return response;
    }

    /**
     * Xóa vĩnh viễn nhóm và toàn bộ dữ liệu thuộc nhóm.
     * Chỉ user đang là owner/leader của nhóm mới được thực hiện chức năng này.
     */
    @Transactional
    public DeleteGroupResponse deleteGroup(
            String groupId,
            JwtAuthenticationToken authentication
    ) {
        // Lấy user từ JWT và lấy nhóm cần xóa từ DB.
        User currentUser = getCurrentUser(authentication);
        StudyGroup group = getGroupById(groupId);

        // Owner là leader duy nhất có quyền xóa toàn bộ nhóm.
        if (!group.getOwner().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.GROUP_LEADER_PERMISSION_REQUIRED);
        }

        // Giữ lại thông tin nhóm vì sau khi xóa DB sẽ không thể đọc lại entity này.
        String deletedGroupId = group.getId();
        String deletedGroupName = group.getName();
        String deletedAt = LocalDateTime.now().toString();

        // Giữ URL file để listener có thể xóa file vật lý sau khi transaction DB commit.
        List<String> storageUrls = groupFileRepo.findByGroupIdOrderByUploadedAtDesc(deletedGroupId)
                .stream()
                .map(GroupFile::getStorageUrl)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        // Tạo thông báo trước khi xóa membership để lấy được đầy đủ danh sách member.
        // Leader bị loại khỏi danh sách nhận vì chính leader là người thực hiện thao tác.
        notificationService.createForGroupMembers(
                group,
                currentUser,
                NotificationType.GROUP_DELETED,
                "Group deleted",
                "Group \"" + deletedGroupName + "\" was deleted by the leader.",
                NotificationType.GROUP,
                deletedGroupId,
                deletedGroupName,
                Set.of(currentUser.getId())
        );

        // Xóa bảng con trước để không vi phạm khóa ngoại trỏ tới study_group.
        groupMessageRepo.deleteByGroupId(deletedGroupId);
        groupFileRepo.deleteByGroupId(deletedGroupId);
        groupMemberRepo.deleteByGroupId(deletedGroupId);
        studyGroupRepo.deleteExistingById(deletedGroupId);

        // Đẩy các lệnh xóa xuống DB ngay trong transaction để phát hiện lỗi trước khi trả response.
        studyGroupRepo.flush();

        // Listener chỉ xử lý realtime và Cloudinary sau khi transaction commit thành công.
        eventPublisher.publishEvent(new GroupDeletedEvent(
                deletedGroupId,
                deletedGroupName,
                deletedAt,
                storageUrls
        ));

        // Trả thông tin gọn để FE hiển thị toast và cập nhật danh sách nhóm.
        return DeleteGroupResponse.builder()
                .groupId(deletedGroupId)
                .groupName(deletedGroupName)
                .deletedAt(deletedAt)
                .build();
    }

    @Transactional
    public CreateGroupResponse regenerateInviteToken(
            String groupId,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        requireLeader(groupId, currentUser);

        StudyGroup group = getGroupById(groupId);
        group.setInviteToken(generateInviteToken());
        group.setInviteEnabled(true);
        group.setUpdatedAt(LocalDateTime.now());

        return buildCreateGroupResponse(studyGroupRepo.save(group));
    }

    @Transactional
    public CreateGroupResponse updateGroupPassword(
            String groupId,
            UpdateGroupPasswordRequest request,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        GroupMember leader = requireLeader(groupId, currentUser);
        StudyGroup group = leader.getGroup();
        String newPassword = normalizeRequiredGroupPassword(request == null ? null : request.getNewPassword());
        String confirmPassword = normalizeRequiredGroupPassword(request == null ? null : request.getConfirmPassword());

        // Kiem tra confirm truoc khi ma hoa password moi.
        if (!newPassword.equals(confirmPassword)) {
            throw new AppException(ErrorCode.GROUP_CONFIRM_PASSWORD_NOT_MATCH);
        }

        // BCrypt khong giai ma nguoc, nen dung matches de so voi hash cu.
        if (passwordEncoder.matches(newPassword, group.getPassword())) {
            throw new AppException(ErrorCode.GROUP_NEW_PASSWORD_SAME_AS_OLD);
        }

        group.setPassword(passwordEncoder.encode(newPassword));
        group.setUpdatedAt(LocalDateTime.now());

        StudyGroup savedGroup = studyGroupRepo.save(group);
        notificationService.createForGroupMembers(
                savedGroup,
                currentUser,
                NotificationType.GROUP_PASSWORD_CHANGED,
                "Group password changed",
                "The password for group \"" + savedGroup.getName() + "\" has been updated.",
                NotificationType.GROUP,
                savedGroup.getId(),
                savedGroup.getName(),
                Set.of(currentUser.getId())
        );

        return buildCreateGroupResponse(savedGroup);
    }

    @Transactional
    public GroupMembersResponse getMembers(
            String groupId,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        GroupMember leader = requireLeader(groupId, currentUser);
        StudyGroup group = leader.getGroup();
        List<GroupMemberResponse> members = groupMemberRepo.findByGroupIdOrderByJoinedAtAsc(group.getId())
                .stream()
                .map(this::buildGroupMemberResponse)
                .toList();

        return GroupMembersResponse.builder()
                .groupId(group.getId())
                .groupName(group.getName())
                .totalMembers((long) members.size())
                .members(members)
                .build();
    }

    private User getCurrentUser(JwtAuthenticationToken authentication) {
        return userRepo.findById(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private StudyGroup getGroupById(String groupId) {
        return studyGroupRepo.findById(normalizeRequiredText(groupId))
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
    }

    private StudyGroup getGroupByInviteToken(String inviteToken) {
        return studyGroupRepo.findByInviteToken(normalizeRequiredText(inviteToken))
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
    }

    private void ensureInviteEnabled(StudyGroup group) {
        if (!Boolean.TRUE.equals(group.getInviteEnabled())) {
            throw new AppException(ErrorCode.GROUP_INVITE_DISABLED);
        }
    }

    private GroupMember requireLeader(String groupId, User currentUser) {
        String normalizedGroupId = normalizeRequiredText(groupId);
        GroupMember member = groupMemberRepo.findByGroupIdAndUserId(normalizedGroupId, currentUser.getId())
                .orElse(null);

        if (member == null) {
            StudyGroup group = getGroupById(normalizedGroupId);
            if (group.getOwner().getId().equals(currentUser.getId())) {
                return createOwnerLeaderMembership(group, currentUser);
            }

            throw new AppException(ErrorCode.GROUP_ACCESS_DENIED);
        }

        if (member.getGroup().getOwner().getId().equals(currentUser.getId())
                && member.getRole() != GroupRole.LEADER) {
            return syncOwnerLeaderMembership(member);
        }

        if (member.getRole() != GroupRole.LEADER) {
            throw new AppException(ErrorCode.GROUP_ACCESS_DENIED);
        }

        return member;
    }

    private GroupMember requireLeaderForPermission(String groupId, User currentUser) {
        String normalizedGroupId = normalizeRequiredText(groupId);
        GroupMember member = groupMemberRepo.findByGroupIdAndUserId(normalizedGroupId, currentUser.getId())
                .orElse(null);

        if (member == null) {
            StudyGroup group = getGroupById(normalizedGroupId);
            if (group.getOwner().getId().equals(currentUser.getId())) {
                return createOwnerLeaderMembership(group, currentUser);
            }

            throw new AppException(ErrorCode.GROUP_LEADER_PERMISSION_REQUIRED);
        }

        if (member.getGroup().getOwner().getId().equals(currentUser.getId())
                && member.getRole() != GroupRole.LEADER) {
            return syncOwnerLeaderMembership(member);
        }

        if (member.getRole() != GroupRole.LEADER) {
            throw new AppException(ErrorCode.GROUP_LEADER_PERMISSION_REQUIRED);
        }

        return member;
    }

    private GroupMember requireApprovedMember(String groupId, User currentUser) {
        String normalizedGroupId = normalizeRequiredText(groupId);
        GroupMember member = groupMemberRepo.findByGroupIdAndUserId(normalizedGroupId, currentUser.getId())
                .orElse(null);

        if (member == null) {
            StudyGroup group = getGroupById(normalizedGroupId);
            if (group.getOwner().getId().equals(currentUser.getId())) {
                return createOwnerLeaderMembership(group, currentUser);
            }

            throw new AppException(ErrorCode.GROUP_ACCESS_DENIED);
        }

        if (member.getGroup().getOwner().getId().equals(currentUser.getId())
                && member.getRole() != GroupRole.LEADER) {
            return syncOwnerLeaderMembership(member);
        }

        return member;
    }

    private GroupMember createOwnerLeaderMembership(StudyGroup group, User owner) {
        GroupMember member = GroupMember.builder()
                .group(group)
                .user(owner)
                .role(GroupRole.LEADER)
                .canUpload(true)
                .canChat(true)
                .joinedAt(LocalDateTime.now())
                .build();

        return groupMemberRepo.save(member);
    }

    private GroupMember syncOwnerLeaderMembership(GroupMember member) {
        member.setRole(GroupRole.LEADER);
        member.setCanUpload(true);
        member.setCanChat(true);
        if (member.getJoinedAt() == null) {
            member.setJoinedAt(LocalDateTime.now());
        }

        return groupMemberRepo.save(member);
    }

    private GroupMember getMemberInGroup(String groupId, String memberId) {
        GroupMember member = groupMemberRepo.findById(normalizeRequiredText(memberId))
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        if (!member.getGroup().getId().equals(normalizeRequiredText(groupId))) {
            throw new AppException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

        return member;
    }

    private String generateInviteToken() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String token = UUID.randomUUID().toString();
            if (!studyGroupRepo.existsByInviteToken(token)) {
                return token;
            }
        }

        throw new AppException(ErrorCode.GROUP_INVITE_TOKEN_GENERATION_FAILED);
    }

    private CreateGroupResponse buildCreateGroupResponse(StudyGroup group) {
        return CreateGroupResponse.builder()
                .groupId(group.getId())
                .name(group.getName())
                .inviteLink(buildInviteLink(group.getInviteToken()))
                .build();
    }

    /**
     * Dong goi thong tin group gon cho danh sach "Group cua toi".
     */
    private GroupSummaryResponse buildGroupSummaryResponse(
            StudyGroup group,
            GroupMember member,
            User currentUser
    ) {
        boolean isLeader = group.getOwner().getId().equals(currentUser.getId())
                || (member != null && member.getRole() == GroupRole.LEADER);
        String role = isLeader ? GroupRole.LEADER.name() : member.getRole().name();

        return GroupSummaryResponse.builder()
                .groupId(group.getId())
                .name(group.getName())
                .description(group.getDescription())
                .ownerId(group.getOwner().getId())
                .ownerName(group.getOwner().getFullname())
                .memberId(member == null ? null : member.getId())
                .role(role)
                .canUpload(isLeader || (member != null && Boolean.TRUE.equals(member.getCanUpload())))
                .canChat(isLeader || member == null || Boolean.TRUE.equals(member.getCanChat()))
                .inviteEnabled(group.getInviteEnabled())
                .inviteLink(isLeader ? buildInviteLink(group.getInviteToken()) : null)
                .memberCount(groupMemberRepo.countByGroupId(group.getId()))
                .activeFileCount(groupFileRepo.countByGroupIdAndDeletedFalse(group.getId()))
                .trashFileCount(groupFileRepo.countByGroupIdAndDeletedTrue(group.getId()))
                .createdAt(group.getCreatedAt() == null ? null : group.getCreatedAt().toString())
                .updatedAt(group.getUpdatedAt() == null ? null : group.getUpdatedAt().toString())
                .build();
    }

    private String buildInviteLink(String inviteToken) {
        String baseUrl = inviteBaseUrl == null || inviteBaseUrl.isBlank()
                ? "http://localhost:3000/join"
                : inviteBaseUrl.trim();

        return baseUrl.endsWith("/") ? baseUrl + inviteToken : baseUrl + "/" + inviteToken;
    }

    private GroupMemberResponse buildGroupMemberResponse(GroupMember member) {
        User user = member.getUser();

        return GroupMemberResponse.builder()
                .memberId(member.getId())
                .userId(user.getId())
                .username(user.getUsername())
                .fullname(user.getFullname())
                .email(user.getEmail())
                .avatarUrl(user.getAvatarUrl())
                .role(member.getRole().name())
                .canUpload(Boolean.TRUE.equals(member.getCanUpload()))
                .canChat(Boolean.TRUE.equals(member.getCanChat()))
                .joinedAt(member.getJoinedAt() == null ? null : member.getJoinedAt().toString())
                .build();
    }

    private String normalizeRequiredText(String value) {
        String normalizedValue = normalizeOptionalText(value);
        if (normalizedValue == null) {
            throw new AppException(ErrorCode.REQUEST_BODY_INVALID);
        }

        return normalizedValue;
    }

    private String normalizeRequiredGroupPassword(String value) {
        String normalizedValue = normalizeOptionalText(value);
        if (normalizedValue == null) {
            throw new AppException(ErrorCode.GROUP_PASSWORD_INVALID);
        }

        return normalizedValue;
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizePageSize(int size) {
        if (size < 1) {
            return 5;
        }

        return Math.min(size, 50);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String cleanValue = StringUtils.cleanPath(value).trim();
        return cleanValue.isBlank() ? null : cleanValue;
    }
}
