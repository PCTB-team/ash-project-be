package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.CreateGroupRequest;
import com.pctb.webapp.dto.request.JoinGroupRequest;
import com.pctb.webapp.dto.request.UpdateUploadPermissionRequest;
import com.pctb.webapp.dto.response.CreateGroupResponse;
import com.pctb.webapp.dto.response.GroupMemberResponse;
import com.pctb.webapp.dto.response.GroupPreviewResponse;
import com.pctb.webapp.entity.GroupMember;
import com.pctb.webapp.entity.GroupRole;
import com.pctb.webapp.entity.JoinStatus;
import com.pctb.webapp.entity.StudyGroup;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupService {
    StudyGroupRepo studyGroupRepo;

    GroupMemberRepo groupMemberRepo;

    UserRepo userRepo;

    PasswordEncoder passwordEncoder;

    @Value("${app.group.invite-base-url:http://localhost:3000/join}")
    @NonFinal
    String inviteBaseUrl;

    /**
     * Tao private group moi va tao membership LEADER cho nguoi tao group.
     * Leader duoc APPROVED va canUpload=true ngay tu dau.
     */
    @Transactional
    public CreateGroupResponse createGroup(
            CreateGroupRequest request,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        String name = normalizeRequiredText(request == null ? null : request.getName());
        String password = normalizeRequiredText(request == null ? null : request.getPassword());
        String description = normalizeOptionalText(request == null ? null : request.getDescription());
        String encodedPassword = passwordEncoder.encode(password);
        LocalDateTime now = LocalDateTime.now();

        StudyGroup group = StudyGroup.builder()
                .name(name)
                .description(description)
                .passwordHash(encodedPassword)
                .inviteToken(generateInviteToken())
                .joinCode(generateJoinCode())
                .visibility("PRIVATE")
                .password(encodedPassword)
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
                .joinStatus(JoinStatus.APPROVED)
                .canUpload(true)
                .joinedAt(now)
                .build();
        groupMemberRepo.save(leaderMember);

        return buildCreateGroupResponse(savedGroup);
    }

    /**
     * Lay thong tin group tu inviteToken de frontend hien preview link moi.
     * Khong bao gio tra passwordHash ve client.
     */
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

    /**
     * User nhap password tu invite link de gui yeu cau join group.
     * Neu hop le thi tao hoac chuyen membership sang PENDING, canUpload=false.
     */
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
        if (!passwordEncoder.matches(password, group.getPasswordHash())) {
            throw new AppException(ErrorCode.GROUP_PASSWORD_INCORRECT);
        }

        GroupMember existingMember = groupMemberRepo.findByGroupAndUser(group, currentUser)
                .orElse(null);
        if (existingMember != null) {
            handleExistingJoinRequest(existingMember);
            return;
        }

        GroupMember member = GroupMember.builder()
                .group(group)
                .user(currentUser)
                .role(GroupRole.MEMBER)
                .joinStatus(JoinStatus.PENDING)
                .canUpload(false)
                .build();
        groupMemberRepo.save(member);
    }

    /**
     * Leader xem danh sach member dang cho duyet trong group.
     * Moi request quan ly group deu phai qua requireLeader.
     */
    @Transactional
    public List<GroupMemberResponse> getPendingMembers(
            String groupId,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        requireLeader(groupId, currentUser);

        return groupMemberRepo.findByGroupIdAndJoinStatus(groupId, JoinStatus.PENDING)
                .stream()
                .map(this::buildGroupMemberResponse)
                .toList();
    }

    /**
     * Leader duyet mot request join.
     * Chi member dang PENDING moi duoc chuyen sang APPROVED.
     */
    @Transactional
    public GroupMemberResponse approveMember(
            String groupId,
            String memberId,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        requireLeader(groupId, currentUser);

        GroupMember member = getMemberInGroup(groupId, memberId);
        if (member.getJoinStatus() != JoinStatus.PENDING) {
            throw new AppException(ErrorCode.GROUP_MEMBER_NOT_PENDING);
        }

        member.setJoinStatus(JoinStatus.APPROVED);
        member.setJoinedAt(LocalDateTime.now());
        member.setCanUpload(false);

        return buildGroupMemberResponse(groupMemberRepo.save(member));
    }

    /**
     * Leader tu choi mot request join.
     * Member bi REJECTED khong duoc upload va joinedAt bi xoa.
     */
    @Transactional
    public GroupMemberResponse rejectMember(
            String groupId,
            String memberId,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        requireLeader(groupId, currentUser);

        GroupMember member = getMemberInGroup(groupId, memberId);
        if (member.getJoinStatus() == JoinStatus.REJECTED) {
            throw new AppException(ErrorCode.GROUP_MEMBER_ALREADY_REJECTED);
        }

        if (member.getJoinStatus() != JoinStatus.PENDING) {
            throw new AppException(ErrorCode.GROUP_MEMBER_NOT_PENDING);
        }

        member.setJoinStatus(JoinStatus.REJECTED);
        member.setCanUpload(false);
        member.setJoinedAt(null);

        return buildGroupMemberResponse(groupMemberRepo.save(member));
    }

    /**
     * Leader bat hoac tat quyen upload cua member da duoc APPROVED.
     * Khong cho cap quyen upload cho member con PENDING/REJECTED/BANNED.
     */
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
        if (member.getJoinStatus() != JoinStatus.APPROVED) {
            throw new AppException(ErrorCode.GROUP_MEMBER_NOT_APPROVED);
        }

        member.setCanUpload(request.getCanUpload());
        return buildGroupMemberResponse(groupMemberRepo.save(member));
    }

    /**
     * Leader tao inviteToken moi khi link cu bi lo.
     * Token cu se mat tac dung vi da bi replace trong database.
     */
    @Transactional
    public CreateGroupResponse regenerateInviteToken(
            String groupId,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        requireLeader(groupId, currentUser);

        StudyGroup group = getGroupById(groupId);
        group.setInviteToken(generateInviteToken());
        group.setJoinCode(generateJoinCode());
        group.setInviteEnabled(true);
        group.setUpdatedAt(LocalDateTime.now());

        return buildCreateGroupResponse(studyGroupRepo.save(group));
    }

    /**
     * Lay current user tu JWT.
     * Trong project nay authentication.getName() chinh la userId.
     */
    private User getCurrentUser(JwtAuthenticationToken authentication) {
        return userRepo.findById(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * Tim group bang id noi bo.
     * Dung cho cac API quan ly sau khi user da login.
     */
    private StudyGroup getGroupById(String groupId) {
        return studyGroupRepo.findById(normalizeRequiredText(groupId))
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
    }

    /**
     * Tim group bang inviteToken.
     * Dung cho preview link va join bang link moi.
     */
    private StudyGroup getGroupByInviteToken(String inviteToken) {
        return studyGroupRepo.findByInviteToken(normalizeRequiredText(inviteToken))
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
    }

    /**
     * Check invite link con duoc bat hay khong.
     * Neu leader tat/regenerate link thi link cu khong duoc join nua.
     */
    private void ensureInviteEnabled(StudyGroup group) {
        if (!Boolean.TRUE.equals(group.getInviteEnabled())) {
            throw new AppException(ErrorCode.GROUP_INVITE_DISABLED);
        }
    }

    /**
     * Xu ly khi user da co record membership trong group.
     * PENDING/APPROVED/BANNED se tra loi ro, REJECTED/LEFT duoc gui request lai.
     */
    private void handleExistingJoinRequest(GroupMember member) {
        if (member.getJoinStatus() == JoinStatus.PENDING) {
            throw new AppException(ErrorCode.GROUP_JOIN_REQUEST_PENDING);
        }

        if (member.getJoinStatus() == JoinStatus.APPROVED) {
            throw new AppException(ErrorCode.USER_ALREADY_IN_GROUP);
        }

        if (member.getJoinStatus() == JoinStatus.BANNED) {
            throw new AppException(ErrorCode.GROUP_MEMBER_BANNED);
        }

        member.setRole(GroupRole.MEMBER);
        member.setJoinStatus(JoinStatus.PENDING);
        member.setCanUpload(false);
        member.setJoinedAt(null);
        groupMemberRepo.save(member);
    }

    /**
     * Bat buoc current user phai la LEADER va da APPROVED trong group.
     * Neu user la owner nhung membership bi thieu/le do data cu, ham nay se dong bo lai.
     */
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
                && (member.getRole() != GroupRole.LEADER || member.getJoinStatus() != JoinStatus.APPROVED)) {
            return syncOwnerLeaderMembership(member);
        }

        if (member.getRole() != GroupRole.LEADER || member.getJoinStatus() != JoinStatus.APPROVED) {
            throw new AppException(ErrorCode.GROUP_ACCESS_DENIED);
        }

        return member;
    }

    /**
     * Tao membership LEADER cho owner neu DB cu chua co record group_member.
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
     * Sua membership cua owner thanh LEADER APPROVED khi data cu bi lech role/status.
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
     * Tim member theo memberId va dam bao member do thuoc dung groupId.
     * Chan viec leader group A sua member cua group B.
     */
    private GroupMember getMemberInGroup(String groupId, String memberId) {
        GroupMember member = groupMemberRepo.findById(normalizeRequiredText(memberId))
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_MEMBER_NOT_FOUND));

        if (!member.getGroup().getId().equals(normalizeRequiredText(groupId))) {
            throw new AppException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

        return member;
    }

    /**
     * Sinh inviteToken UUID va check trung database.
     * UUID rat kho trung, nhung van check de tranh loi unique constraint.
     */
    private String generateInviteToken() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String token = UUID.randomUUID().toString();
            if (!studyGroupRepo.existsByInviteToken(token)) {
                return token;
            }
        }

        throw new AppException(ErrorCode.GROUP_INVITE_TOKEN_GENERATION_FAILED);
    }

    /**
     * Sinh joinCode ngan cho cot legacy join_code trong DB cu.
     * Invite link moi khong dung gia tri nay, nhung MySQL cu van bat buoc NOT NULL.
     */
    private String generateJoinCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            String code = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 12)
                    .toUpperCase();
            if (!studyGroupRepo.existsByJoinCode(code)) {
                return code;
            }
        }

        throw new AppException(ErrorCode.GROUP_INVITE_TOKEN_GENERATION_FAILED);
    }

    /**
     * Tao response tra ve sau khi tao group hoac regenerate invite token.
     */
    private CreateGroupResponse buildCreateGroupResponse(StudyGroup group) {
        return CreateGroupResponse.builder()
                .groupId(group.getId())
                .name(group.getName())
                .inviteLink(buildInviteLink(group.getInviteToken()))
                .build();
    }

    /**
     * Ghep inviteToken vao base URL de FE co link share cho user khac.
     */
    private String buildInviteLink(String inviteToken) {
        String baseUrl = inviteBaseUrl == null || inviteBaseUrl.isBlank()
                ? "http://localhost:3000/join"
                : inviteBaseUrl.trim();

        return baseUrl.endsWith("/") ? baseUrl + inviteToken : baseUrl + "/" + inviteToken;
    }

    /**
     * Convert GroupMember thanh DTO cho FE, khong tra ve object entity lazy.
     */
    private GroupMemberResponse buildGroupMemberResponse(GroupMember member) {
        User user = member.getUser();

        return GroupMemberResponse.builder()
                .memberId(member.getId())
                .userId(user.getId())
                .fullname(user.getFullname())
                .email(user.getEmail())
                .role(member.getRole().name())
                .joinStatus(member.getJoinStatus().name())
                .canUpload(Boolean.TRUE.equals(member.getCanUpload()))
                .build();
    }

    /**
     * Chuan hoa text bat buoc, tranh null/blank va path traversal tu input.
     */
    private String normalizeRequiredText(String value) {
        String normalizedValue = normalizeOptionalText(value);
        if (normalizedValue == null) {
            throw new AppException(ErrorCode.REQUEST_BODY_INVALID);
        }

        return normalizedValue;
    }

    /**
     * Chuan hoa text tuy chon, null/blank se tra ve null.
     */
    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String cleanValue = StringUtils.cleanPath(value).trim();
        return cleanValue.isBlank() ? null : cleanValue;
    }
}
