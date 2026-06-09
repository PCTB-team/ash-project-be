package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.CreateGroupRequest;
import com.pctb.webapp.dto.request.JoinGroupRequest;
import com.pctb.webapp.dto.request.UpdateUploadPermissionRequest;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.CreateGroupResponse;
import com.pctb.webapp.dto.response.GroupMemberResponse;
import com.pctb.webapp.dto.response.GroupPreviewResponse;
import com.pctb.webapp.service.GroupService;
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
     * Xem preview group bang inviteToken.
     * API nay khong tra passwordHash.
     */
    @GetMapping("/invite/{inviteToken}")
    public ApiResponse<GroupPreviewResponse> getGroupPreview(@PathVariable String inviteToken) {
        return ApiResponse.<GroupPreviewResponse>builder()
                .message("Get group preview successfully")
                .result(groupService.getGroupPreview(inviteToken))
                .build();
    }

    /**
     * User gui yeu cau join group bang inviteToken va password group.
     * Neu hop le, request se o trang thai PENDING.
     */
    @PostMapping("/invite/{inviteToken}/join")
    public ApiResponse<String> joinByInvite(
            @PathVariable String inviteToken,
            @RequestBody @Valid JoinGroupRequest request,
            JwtAuthenticationToken authentication
    ) {
        groupService.joinByInvite(inviteToken, request, authentication);

        return ApiResponse.<String>builder()
                .message("Join request sent successfully")
                .result("PENDING")
                .build();
    }

    /**
     * Leader xem danh sach member dang cho duyet.
     */
    @GetMapping("/{groupId}/pending-members")
    public ApiResponse<List<GroupMemberResponse>> getPendingMembers(
            @PathVariable String groupId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<List<GroupMemberResponse>>builder()
                .message("Get pending members successfully")
                .result(groupService.getPendingMembers(groupId, authentication))
                .build();
    }

    /**
     * Leader duyet member vao group.
     * Sau khi approve, member van can leader bat canUpload de upload file.
     */
    @PutMapping("/{groupId}/members/{memberId}/approve")
    public ApiResponse<GroupMemberResponse> approveMember(
            @PathVariable String groupId,
            @PathVariable String memberId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<GroupMemberResponse>builder()
                .message("Approve member successfully")
                .result(groupService.approveMember(groupId, memberId, authentication))
                .build();
    }

    /**
     * Leader tu choi request join cua member.
     */
    @PutMapping("/{groupId}/members/{memberId}/reject")
    public ApiResponse<GroupMemberResponse> rejectMember(
            @PathVariable String groupId,
            @PathVariable String memberId,
            JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<GroupMemberResponse>builder()
                .message("Reject member successfully")
                .result(groupService.rejectMember(groupId, memberId, authentication))
                .build();
    }

    /**
     * Leader bat hoac tat quyen upload file cua member da APPROVED.
     */
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
     * Leader tao inviteToken moi khi link cu bi lo.
     */
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
}
