package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.CreateGroupRequest;
import com.pctb.webapp.dto.request.JoinGroupRequest;
import com.pctb.webapp.dto.request.UpdateGroupPasswordRequest;
import com.pctb.webapp.dto.request.UpdateUploadPermissionRequest;
import com.pctb.webapp.dto.response.CreateGroupResponse;
import com.pctb.webapp.dto.response.GroupMemberResponse;
import com.pctb.webapp.dto.response.GroupMembersResponse;
import com.pctb.webapp.dto.response.GroupPreviewResponse;
import com.pctb.webapp.dto.response.GroupSummaryResponse;
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
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
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

    UserRepo userRepo;

    PasswordEncoder passwordEncoder;

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
                .joinedAt(now)
                .build();
        groupMemberRepo.save(leaderMember);

        return buildCreateGroupResponse(savedGroup);
    }

    /**
     * Lay cac group ma user dang dang nhap dang tham gia hoac dang lam leader.
     */
    @Transactional(readOnly = true)
    public List<GroupSummaryResponse> getMyGroups(
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        List<GroupSummaryResponse> responses = new ArrayList<>();
        Set<String> addedGroupIds = new HashSet<>();

        groupMemberRepo.findByUserIdOrderByJoinedAtDesc(currentUser.getId())
                .forEach(member -> {
                    StudyGroup group = member.getGroup();
                    if (addedGroupIds.add(group.getId())) {
                        responses.add(buildGroupSummaryResponse(group, member, currentUser));
                    }
                });

        studyGroupRepo.findByOwnerIdOrderByCreatedAtDesc(currentUser.getId())
                .forEach(group -> {
                    if (addedGroupIds.add(group.getId())) {
                        responses.add(buildGroupSummaryResponse(group, null, currentUser));
                    }
                });

        return responses;
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
                .joinedAt(LocalDateTime.now())
                .build();
        groupMemberRepo.save(member);
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
        GroupMemberResponse response = buildGroupMemberResponse(member);
        groupMemberRepo.delete(member);

        return response;
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

        return buildCreateGroupResponse(studyGroupRepo.save(group));
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

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String cleanValue = StringUtils.cleanPath(value).trim();
        return cleanValue.isBlank() ? null : cleanValue;
    }
}
