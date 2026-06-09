package com.pctb.webapp;

import com.pctb.webapp.dto.request.CreateGroupRequest;
import com.pctb.webapp.dto.request.JoinGroupRequest;
import com.pctb.webapp.dto.request.UpdateUploadPermissionRequest;
import com.pctb.webapp.dto.response.CreateGroupResponse;
import com.pctb.webapp.dto.response.GroupMemberResponse;
import com.pctb.webapp.entity.GroupMember;
import com.pctb.webapp.entity.GroupRole;
import com.pctb.webapp.entity.JoinStatus;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.GroupMemberRepo;
import com.pctb.webapp.repository.UserRepo;
import com.pctb.webapp.service.GroupFileService;
import com.pctb.webapp.service.GroupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(leaderMembership.getJoinStatus()).isEqualTo(JoinStatus.APPROVED);
        assertThat(leaderMembership.getCanUpload()).isTrue();

        groupMemberRepo.delete(leaderMembership);
        groupMemberRepo.flush();
        assertThat(groupService.getPendingMembers(groupId, leaderAuth)).isEmpty();
        GroupMember healedLeaderMembership = groupMemberRepo.findByGroupIdAndUserId(groupId, leader.getId()).orElseThrow();
        assertThat(healedLeaderMembership.getRole()).isEqualTo(GroupRole.LEADER);
        assertThat(healedLeaderMembership.getJoinStatus()).isEqualTo(JoinStatus.APPROVED);
        assertThat(healedLeaderMembership.getCanUpload()).isTrue();

        groupService.joinByInvite(
                inviteToken,
                JoinGroupRequest.builder().password("123123@").build(),
                memberAuth
        );

        List<GroupMemberResponse> pendingMembers = groupService.getPendingMembers(groupId, leaderAuth);
        assertThat(pendingMembers).hasSize(1);
        String memberId = pendingMembers.getFirst().getMemberId();
        assertThat(pendingMembers.getFirst().getJoinStatus()).isEqualTo(JoinStatus.PENDING.name());

        assertThatThrownBy(() -> groupService.getPendingMembers(groupId, memberAuth))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.GROUP_ACCESS_DENIED);

        GroupMemberResponse approvedMember = groupService.approveMember(groupId, memberId, leaderAuth);
        assertThat(approvedMember.getJoinStatus()).isEqualTo(JoinStatus.APPROVED.name());
        assertThat(approvedMember.getCanUpload()).isFalse();

        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                "hello group".getBytes()
        );
        assertThatThrownBy(() -> groupFileService.uploadFile(groupId, textFile, memberAuth))
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
        assertThat(groupFileService.uploadFile(groupId, uploadFile, memberAuth).getFileName())
                .isEqualTo("note.txt");
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
