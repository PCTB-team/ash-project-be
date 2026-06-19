package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.CreateGroupRequest;
import com.pctb.webapp.dto.request.JoinGroupRequest;
import com.pctb.webapp.dto.request.UpdateGroupPasswordRequest;
import com.pctb.webapp.dto.request.UpdateUploadPermissionRequest;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.CreateGroupResponse;
import com.pctb.webapp.dto.response.GroupMemberResponse;
import com.pctb.webapp.dto.response.GroupMembersResponse;
import com.pctb.webapp.dto.response.GroupPreviewResponse;
import com.pctb.webapp.dto.response.GroupSummaryResponse;
import com.pctb.webapp.service.GroupService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupController {
    GroupService groupService;

    /**
     * Tao private group moi.
     * Leader nhan ve groupId noi bo va inviteLink de share cho user khac.
     */
    @Operation(summary = "Create private group")
    @PostMapping
    public ApiResponse<CreateGroupResponse> createGroup(
            @RequestBody @Valid CreateGroupRequest request,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<CreateGroupResponse>builder()
                .message("Create group successfully")
                .result(groupService.createGroup(request, authentication))
                .build();
    }

    /**
     * Lay danh sach group cua user dang dang nhap.
     * FE dung API nay de render man hinh "Group cua toi".
     */
    @Operation(summary = "Get my groups")
    @GetMapping("/my")
    public ApiResponse<List<GroupSummaryResponse>> getMyGroups(
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<List<GroupSummaryResponse>>builder()
                .message("Get my groups successfully")
                .result(groupService.getMyGroups(authentication))
                .build();
    }

    /**
     * Lay chi tiet mot group ma user dang tham gia.
     * FE dung API nay khi refresh thang trang detail group.
     */
    @Operation(summary = "Get group detail")
    @GetMapping("/{groupId}")
    public ApiResponse<GroupSummaryResponse> getGroupDetail(
            @PathVariable String groupId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<GroupSummaryResponse>builder()
                .message("Get group detail successfully")
                .result(groupService.getGroupDetail(groupId, authentication))
                .build();
    }

    /**
     * Xem preview group bang inviteToken.
     * API nay khong tra password.
     */
    @Operation(summary = "Get group preview by invite token")
    @GetMapping("/invite/{inviteToken}")
    public ApiResponse<GroupPreviewResponse> getGroupPreview(@PathVariable String inviteToken) {
        return ApiResponse.<GroupPreviewResponse>builder()
                .message("Get group preview successfully")
                .result(groupService.getGroupPreview(inviteToken))
                .build();
    }

    /**
     * User join group bang inviteToken va password group.
     * Neu hop le, member vao group ngay nhung chua co quyen upload file.
     */
    @Operation(summary = "Join group by invite token")
    @PostMapping("/invite/{inviteToken}/join")
    public ApiResponse<String> joinByInvite(
            @PathVariable String inviteToken,
            @RequestBody @Valid JoinGroupRequest request,
            JwtAuthenticationToken authentication
    ) {
        groupService.joinByInvite(inviteToken, request, authentication);

        return ApiResponse.<String>builder()
                .message("Join group successfully")
                .result("APPROVED")
                .build();
    }

    /**
     * Leader bat hoac tat quyen upload file cua member.
     */
    @Operation(summary = "Update group member upload permission")
    @PutMapping("/{groupId}/members/{memberId}/upload-permission")
    public ApiResponse<GroupMemberResponse> updateUploadPermission(
            @PathVariable String groupId,
            @PathVariable String memberId,
            @RequestBody @Valid UpdateUploadPermissionRequest request,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<GroupMemberResponse>builder()
                .message("Update upload permission successfully")
                .result(groupService.updateUploadPermission(groupId, memberId, request, authentication))
                .build();
    }

    /**
     * Leader kick member khoi group.
     * Backend xoa membership record, user co the join lai bang invite link.
     */
    @Operation(summary = "Kick group member")
    @PutMapping("/{groupId}/members/{memberId}/kick")
    public ApiResponse<GroupMemberResponse> kickMember(
            @PathVariable String groupId,
            @PathVariable String memberId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<GroupMemberResponse>builder()
                .message("Kick member successfully")
                .result(groupService.kickMember(groupId, memberId, authentication))
                .build();
    }

    /**
     * Member tu roi khoi group.
     * Backend lay user hien tai tu JWT, FE khong can gui memberId.
     */
    @Operation(summary = "Leave group")
    @PutMapping("/{groupId}/leave")
    public ApiResponse<GroupMemberResponse> leaveGroup(
            @PathVariable String groupId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<GroupMemberResponse>builder()
                .message("Leave group successfully")
                .result(groupService.leaveGroup(groupId, authentication))
                .build();
    }

    /**
     * Leader tao inviteToken moi khi link cu bi lo.
     */
    @Operation(summary = "Regenerate group invite token")
    @PutMapping("/{groupId}/regenerate-invite-token")
    public ApiResponse<CreateGroupResponse> regenerateInviteToken(
            @PathVariable String groupId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<CreateGroupResponse>builder()
                .message("Regenerate invite token successfully")
                .result(groupService.regenerateInviteToken(groupId, authentication))
                .build();
    }

    /**
     * Leader doi password group.
     * Backend chi luu password da ma hoa, khong tra password ra FE.
     */
    @Operation(summary = "Update group password")
    @PutMapping("/{groupId}/password")
    public ApiResponse<CreateGroupResponse> updateGroupPassword(
            @PathVariable String groupId,
            @RequestBody @Valid UpdateGroupPasswordRequest request,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<CreateGroupResponse>builder()
                .message("Update group password successfully")
                .result(groupService.updateGroupPassword(groupId, request, authentication))
                .build();
    }

    /**
     * Leader xem danh sach member de lay memberId kick hoac cap quyen upload.
     */
    @Operation(summary = "Get group members")
    @GetMapping("/{groupId}/members")
    public ApiResponse<GroupMembersResponse> getMembers(
            @PathVariable String groupId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<GroupMembersResponse>builder()
                .message("Get group members successfully")
                .result(groupService.getMembers(groupId, authentication))
                .build();
    }

}
