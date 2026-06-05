package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.GroupCreationRequest;
import com.pctb.webapp.dto.request.GroupJoinRequest;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.GroupResponse;
import com.pctb.webapp.service.GroupService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupController {
    GroupService groupService;

    // API Tạo nhóm học tập mới
    @PostMapping("/create")
    public ApiResponse<GroupResponse> createGroup(@RequestBody @Valid GroupCreationRequest request) {
        return ApiResponse.<GroupResponse>builder()
                .result(groupService.createGroup(request))
                .build();
    }

    // API Tham gia nhóm bằng mã code (Có check Password nếu là nhóm PRIVATE)
    @PostMapping("/join")
    public ApiResponse<String> joinGroup(@RequestBody @Valid GroupJoinRequest request) {
        // ĐÃ SỬA THÀNH joinGroupByCode ĐỂ KHỚP 100% VỚI SERVICE CỦA BẠN
        groupService.joinGroupByCode(request);
        return ApiResponse.<String>builder()
                .result("Joined group successfully")
                .build();
    }

    // API Tham gia trực tiếp không cần code từ danh sách nhóm công khai (Chỉ dành cho PUBLIC)
    @PostMapping("/{groupId}/join-direct")
    public ApiResponse<String> joinPublicGroupDirectly(@PathVariable String groupId) {
        groupService.joinPublicGroupDirectly(groupId);
        return ApiResponse.<String>builder()
                .result("Joined public group successfully")
                .build();
    }
}