package com.pctb.webapp;

import com.pctb.webapp.dto.request.CreateGroupRequest;
import com.pctb.webapp.dto.request.JoinGroupRequest;
import com.pctb.webapp.dto.request.UpdateUploadPermissionRequest;
import com.pctb.webapp.dto.response.CreateGroupResponse;
import com.pctb.webapp.dto.response.GroupFileResponse;
import com.pctb.webapp.dto.response.GroupMemberResponse;
import com.pctb.webapp.dto.response.GroupMembersResponse;
import com.pctb.webapp.dto.response.GroupPreviewResponse;
import com.pctb.webapp.dto.response.GroupStatisticsResponse;
import com.pctb.webapp.entity.GroupMember;
import com.pctb.webapp.entity.GroupRole;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.GroupMemberRepo;
import com.pctb.webapp.repository.UserRepo;
import com.pctb.webapp.service.CloudinaryStorageService;
import com.pctb.webapp.service.GroupFileService;
import com.pctb.webapp.service.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@SpringBootTest
class GroupFlowTests {
    @Autowired
    GroupService groupService;

    @Autowired
    GroupFileService groupFileService;

    @Autowired
    GroupMemberRepo groupMemberRepo;

    @Autowired
    UserRepo userRepo;

    @MockitoBean
    CloudinaryStorageService storageService;

    @BeforeEach
    void mockStorage() {
        lenient().when(storageService.upload(any(), anyString()))
                .thenAnswer(invocation -> "/test-storage/" + invocation.getArgument(1, String.class));
    }

    @Test
    void privateGroupFlowCreatesLeaderPermissionAndAllowsMemberAfterGrant() {
        User leader = saveUser("leader-flow@example.com", "leader-flow");
        User member = saveUser("member-flow@example.com", "member-flow");
        JwtAuthenticationToken leaderAuth = auth(leader.getId());
        JwtAuthenticationToken memberAuth = auth(member.getId());

        CreateGroupResponse createdGroup = groupService.createGroup(
                CreateGroupRequest.builder()
                        .name("Flow Test Group")
                        .description("Flow test")
                        .password("123123@")
                        .build(),
                leaderAuth
        );
        String groupId = createdGroup.getGroupId();
        String inviteToken = createdGroup.getInviteLink().substring(createdGroup.getInviteLink().lastIndexOf('/') + 1);

        GroupMember leaderMembership = groupMemberRepo.findByGroupIdAndUserId(groupId, leader.getId()).orElseThrow();
        assertThat(leaderMembership.getRole()).isEqualTo(GroupRole.LEADER);
        assertThat(leaderMembership.getCanUpload()).isTrue();

        groupMemberRepo.delete(leaderMembership);
        groupMemberRepo.flush();
        GroupStatisticsResponse healedStatistics = groupService.getStatistics(groupId, leaderAuth);
        assertThat(healedStatistics.getTotalMembers()).isEqualTo(1);
        assertThat(healedStatistics.getTotalApprovedMembers()).isEqualTo(1);
        assertThat(groupService.getMemberCount(groupId, leaderAuth)).isEqualTo(1);
        GroupMember healedLeaderMembership = groupMemberRepo.findByGroupIdAndUserId(groupId, leader.getId()).orElseThrow();
        assertThat(healedLeaderMembership.getRole()).isEqualTo(GroupRole.LEADER);
        assertThat(healedLeaderMembership.getCanUpload()).isTrue();

        groupService.joinByInvite(
                inviteToken,
                JoinGroupRequest.builder().password("123123@").build(),
                memberAuth
        );

        GroupMember joinedMember = groupMemberRepo.findByGroupIdAndUserId(groupId, member.getId()).orElseThrow();
        String memberId = joinedMember.getId();
        assertThat(joinedMember.getCanUpload()).isFalse();
        assertThat(joinedMember.getJoinedAt()).isNotNull();

        GroupMembersResponse members = groupService.getMembers(groupId, leaderAuth);
        assertThat(members.getGroupId()).isEqualTo(groupId);
        assertThat(members.getGroupName()).isEqualTo("Flow Test Group");
        assertThat(members.getTotalMembers()).isEqualTo(2);
        assertThat(members.getMembers())
                .extracting(GroupMemberResponse::getMemberId)
                .contains(memberId);
        GroupMemberResponse listedMember = members.getMembers().stream()
                .filter(groupMember -> groupMember.getMemberId().equals(memberId))
                .findFirst()
                .orElseThrow();
        assertThat(listedMember.getUserId()).isEqualTo(member.getId());
        assertThat(listedMember.getUsername()).isEqualTo("member-flow");
        assertThat(listedMember.getFullname()).isEqualTo("member-flow");
        assertThat(listedMember.getEmail()).isEqualTo("member-flow@example.com");
        assertThat(listedMember.getRole()).isEqualTo("MEMBER");
        assertThat(listedMember.getCanUpload()).isFalse();
        assertThat(listedMember.getJoinedAt()).isNotNull();

        assertThatThrownBy(() -> groupService.getMembers(groupId, memberAuth))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GROUP_ACCESS_DENIED);

        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                "hello group".getBytes()
        );
        assertThatThrownBy(() -> groupFileService.uploadFile(groupId, textFile, false, memberAuth))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GROUP_UPLOAD_NOT_ALLOWED);

        GroupMemberResponse uploadEnabledMember = groupService.updateUploadPermission(
                groupId,
                memberId,
                UpdateUploadPermissionRequest.builder().canUpload(true).build(),
                leaderAuth
        );
        assertThat(uploadEnabledMember.getCanUpload()).isTrue();

        MockMultipartFile uploadFile = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                "hello group".getBytes()
        );
        GroupFileResponse firstDocument = groupFileService.uploadFile(groupId, uploadFile, false, memberAuth);
        assertThat(firstDocument.getFileName()).isEqualTo("note.txt");
        assertThat(firstDocument.getDeleted()).isFalse();

        MockMultipartFile duplicateUploadFile = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                "updated group file content".getBytes()
        );
        assertThatThrownBy(() -> groupFileService.uploadFile(groupId, duplicateUploadFile, false, memberAuth))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FILE_ALREADY_EXISTS);

        MockMultipartFile replacementUploadFile = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                "updated group file content".getBytes()
        );
        GroupFileResponse replacedDocument = groupFileService.uploadFile(
                groupId,
                replacementUploadFile,
                true,
                memberAuth
        );
        assertThat(replacedDocument.getFileId()).isEqualTo(firstDocument.getFileId());
        assertThat(replacedDocument.getFileSize()).isEqualTo(replacementUploadFile.getSize());

        MockMultipartFile secondUploadFile = new MockMultipartFile(
                "file",
                "summary.txt",
                "text/plain",
                "hello second group file".getBytes()
        );
        GroupFileResponse secondDocument = groupFileService.uploadFile(groupId, secondUploadFile, false, memberAuth);
        assertThat(secondDocument.getFileName()).isEqualTo("summary.txt");

        assertThat(groupFileService.getGroupFiles(groupId, memberAuth))
                .extracting(GroupFileResponse::getFileId)
                .containsExactlyInAnyOrder(firstDocument.getFileId(), secondDocument.getFileId());

        GroupStatisticsResponse beforeTrashStatistics = groupService.getStatistics(groupId, memberAuth);
        assertThat(beforeTrashStatistics.getTotalMembers()).isEqualTo(2);
        assertThat(beforeTrashStatistics.getTotalApprovedMembers()).isEqualTo(2);
        assertThat(beforeTrashStatistics.getTotalActiveDocuments()).isEqualTo(2);
        assertThat(beforeTrashStatistics.getTotalTrashDocuments()).isZero();
        assertThat(groupService.getMemberCount(groupId, memberAuth)).isEqualTo(2);

        assertThatThrownBy(() -> groupFileService.moveFileToTrash(groupId, firstDocument.getFileId(), memberAuth))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GROUP_ACCESS_DENIED);

        groupFileService.moveFileToTrash(groupId, firstDocument.getFileId(), leaderAuth);
        assertThat(groupFileService.getGroupFiles(groupId, memberAuth))
                .extracting(GroupFileResponse::getFileId)
                .containsExactly(secondDocument.getFileId());
        assertThat(groupFileService.getTrashFiles(groupId, leaderAuth))
                .extracting(GroupFileResponse::getFileId)
                .containsExactly(firstDocument.getFileId());

        GroupStatisticsResponse afterTrashStatistics = groupService.getStatistics(groupId, leaderAuth);
        assertThat(afterTrashStatistics.getTotalMembers()).isEqualTo(2);
        assertThat(afterTrashStatistics.getTotalApprovedMembers()).isEqualTo(2);
        assertThat(afterTrashStatistics.getTotalActiveDocuments()).isEqualTo(1);
        assertThat(afterTrashStatistics.getTotalTrashDocuments()).isEqualTo(1);

        GroupFileResponse restoredDocument = groupFileService.restoreFile(groupId, firstDocument.getFileId(), leaderAuth);
        assertThat(restoredDocument.getDeleted()).isFalse();
        assertThat(restoredDocument.getDeletedAt()).isNull();

        GroupStatisticsResponse afterRestoreStatistics = groupService.getStatistics(groupId, leaderAuth);
        assertThat(afterRestoreStatistics.getTotalActiveDocuments()).isEqualTo(2);
        assertThat(afterRestoreStatistics.getTotalTrashDocuments()).isZero();

        GroupMemberResponse kickedMember = groupService.kickMember(groupId, memberId, leaderAuth);
        assertThat(kickedMember.getCanUpload()).isFalse();
        assertThat(groupMemberRepo.findByGroupIdAndUserId(groupId, member.getId())).isEmpty();
        assertThat(groupService.getMemberCount(groupId, leaderAuth)).isEqualTo(1);

        assertThatThrownBy(() -> groupFileService.getGroupFiles(groupId, memberAuth))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GROUP_ACCESS_DENIED);

        assertThatThrownBy(() -> groupService.getStatistics(groupId, memberAuth))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GROUP_ACCESS_DENIED);
        assertThatThrownBy(() -> groupService.getMemberCount(groupId, memberAuth))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GROUP_ACCESS_DENIED);

        groupService.joinByInvite(
                inviteToken,
                JoinGroupRequest.builder().password("123123@").build(),
                memberAuth
        );

        GroupMember rejoinedMember = groupMemberRepo.findByGroupIdAndUserId(groupId, member.getId()).orElseThrow();
        assertThat(rejoinedMember.getId()).isNotEqualTo(memberId);
        assertThat(rejoinedMember.getCanUpload()).isFalse();
    }

    @Test
    void invitePreviewAndJoinValidationFlow() {
        User leader = saveUser("leader-validation@example.com", "leader-validation");
        User member = saveUser("member-validation@example.com", "member-validation");
        User outsider = saveUser("outsider-validation@example.com", "outsider-validation");
        JwtAuthenticationToken leaderAuth = auth(leader.getId());
        JwtAuthenticationToken memberAuth = auth(member.getId());
        JwtAuthenticationToken outsiderAuth = auth(outsider.getId());

        CreateGroupResponse createdGroup = groupService.createGroup(
                CreateGroupRequest.builder()
                        .name("Validation Group")
                        .description("Validation flow")
                        .password("123123@")
                        .build(),
                leaderAuth
        );
        String groupId = createdGroup.getGroupId();
        String inviteToken = inviteTokenFrom(createdGroup);

        GroupPreviewResponse preview = groupService.getGroupPreview(inviteToken);
        assertThat(preview.getGroupName()).isEqualTo("Validation Group");
        assertThat(preview.getDescription()).isEqualTo("Validation flow");
        assertThat(preview.getOwnerName()).isEqualTo("leader-validation");

        assertThatThrownBy(() -> groupService.joinByInvite(
                inviteToken,
                JoinGroupRequest.builder().password("wrong-password").build(),
                memberAuth
        ))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GROUP_PASSWORD_INCORRECT);

        groupService.joinByInvite(
                inviteToken,
                JoinGroupRequest.builder().password("123123@").build(),
                memberAuth
        );

        assertThatThrownBy(() -> groupService.joinByInvite(
                inviteToken,
                JoinGroupRequest.builder().password("123123@").build(),
                memberAuth
        ))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_ALREADY_IN_GROUP);

        assertThatThrownBy(() -> groupService.getStatistics(groupId, outsiderAuth))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GROUP_ACCESS_DENIED);
        assertThatThrownBy(() -> groupService.getMemberCount(groupId, outsiderAuth))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GROUP_ACCESS_DENIED);

        assertThatThrownBy(() -> groupFileService.getGroupFiles(groupId, outsiderAuth))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GROUP_ACCESS_DENIED);
    }

    @Test
    void leaderOnlyManagementAndInviteRegenerationFlow() {
        User leader = saveUser("leader-management@example.com", "leader-management");
        User member = saveUser("member-management@example.com", "member-management");
        User newMember = saveUser("new-member-management@example.com", "new-member-management");
        JwtAuthenticationToken leaderAuth = auth(leader.getId());
        JwtAuthenticationToken memberAuth = auth(member.getId());
        JwtAuthenticationToken newMemberAuth = auth(newMember.getId());

        CreateGroupResponse createdGroup = groupService.createGroup(
                CreateGroupRequest.builder()
                        .name("Management Group")
                        .description("Management flow")
                        .password("123123@")
                        .build(),
                leaderAuth
        );
        String groupId = createdGroup.getGroupId();
        String inviteToken = inviteTokenFrom(createdGroup);

        groupService.joinByInvite(
                inviteToken,
                JoinGroupRequest.builder().password("123123@").build(),
                memberAuth
        );
        GroupMember memberRecord = groupMemberRepo.findByGroupIdAndUserId(groupId, member.getId()).orElseThrow();
        GroupMember leaderRecord = groupMemberRepo.findByGroupIdAndUserId(groupId, leader.getId()).orElseThrow();

        assertThatThrownBy(() -> groupService.updateUploadPermission(
                groupId,
                memberRecord.getId(),
                UpdateUploadPermissionRequest.builder().canUpload(true).build(),
                memberAuth
        ))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GROUP_ACCESS_DENIED);

        assertThatThrownBy(() -> groupService.kickMember(groupId, leaderRecord.getId(), leaderAuth))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GROUP_LEADER_CANNOT_BE_KICKED);

        assertThatThrownBy(() -> groupService.regenerateInviteToken(groupId, memberAuth))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GROUP_ACCESS_DENIED);

        CreateGroupResponse regenerated = groupService.regenerateInviteToken(groupId, leaderAuth);
        assertThat(regenerated.getInviteLink()).isNotEqualTo(createdGroup.getInviteLink());

        assertThatThrownBy(() -> groupService.getGroupPreview(inviteToken))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GROUP_NOT_FOUND);

        groupService.joinByInvite(
                inviteTokenFrom(regenerated),
                JoinGroupRequest.builder().password("123123@").build(),
                newMemberAuth
        );

        assertThat(groupMemberRepo.findByGroupIdAndUserId(groupId, newMember.getId())).isPresent();
    }

    private User saveUser(String email, String username) {
        return userRepo.findByEmail(email)
                .orElseGet(() -> userRepo.save(User.builder()
                        .email(email)
                        .username(username)
                        .password("encoded-password")
                        .fullname(username)
                        .verified(true)
                        .build()));
    }

    private String inviteTokenFrom(CreateGroupResponse response) {
        return response.getInviteLink().substring(response.getInviteLink().lastIndexOf('/') + 1);
    }

    private JwtAuthenticationToken auth(String userId) {
        Jwt jwt = Jwt.withTokenValue("test-token-" + userId)
                .header("alg", "none")
                .subject(userId)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        return new JwtAuthenticationToken(jwt);
    }
}
