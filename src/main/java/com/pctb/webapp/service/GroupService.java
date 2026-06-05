package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.GroupCreationRequest;
import com.pctb.webapp.dto.request.GroupJoinRequest;
import com.pctb.webapp.dto.response.GroupResponse;
import com.pctb.webapp.entity.*;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.GroupMemberRepo;
import com.pctb.webapp.repository.StudyGroupRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupService {
    StudyGroupRepo groupRepo;
    GroupMemberRepo memberRepo;
    UserRepo userRepo;

    String generateUniqueJoinCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        } while (groupRepo.existsByJoinCode(code));
        return code;
    }

    @Transactional
    public GroupResponse createGroup(GroupCreationRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        String currentUserId = authentication.getName();

        User currentUser = userRepo.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        StudyGroup group = StudyGroup.builder()
                .name(request.getName())
                .description(request.getDescription())
                .visibility(request.getVisibility())
                .owner(currentUser)
                .joinCode(generateUniqueJoinCode())
                .password(request.getPassword()) // ---> LƯU MẬT KHẨU VÀO ĐÂY
                .build();

        StudyGroup savedGroup = groupRepo.save(group);

        GroupMember member = GroupMember.builder()
                .group(savedGroup)
                .user(currentUser)
                .role(GroupRole.OWNER)
                .build();

        memberRepo.save(member);

        return GroupResponse.builder()
                .id(savedGroup.getId())
                .name(savedGroup.getName())
                .description(savedGroup.getDescription())
                .joinCode(savedGroup.getJoinCode())
                .visibility(savedGroup.getVisibility().name())
                .ownerId(currentUser.getId())
                .createdAt(savedGroup.getCreatedAt())
                .build();
    }

    // LUỒNG 1: Tìm bằng mã Code (Public vào thẳng, Private bắt buộc đối chiếu mật khẩu)
    @Transactional
    public void joinGroupByCode(GroupJoinRequest request) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        String currentUserId = authentication.getName();

        User currentUser = userRepo.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        StudyGroup group = groupRepo.findByJoinCode(request.getJoinCode())
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        if (memberRepo.existsByGroupAndUser(group, currentUser)) {
            throw new AppException(ErrorCode.USER_ALREADY_IN_GROUP);
        }

        // ĐỔI LUẬT BẢO MẬT: Nhóm Private thì phải check đúng password mới cho qua
        if (group.getVisibility() == GroupVisibility.PRIVATE) {
            if (request.getPassword() == null || !request.getPassword().equals(group.getPassword())) {
                throw new AppException(ErrorCode.GROUP_IS_PRIVATE); // Trả về lỗi 1114 (Sai pass hoặc ko nhập pass nhóm kín)
            }
        }

        GroupMember member = GroupMember.builder()
                .group(group)
                .user(currentUser)
                .role(GroupRole.MEMBER)
                .build();

        memberRepo.save(member);
    }

    // LUỒNG 2: Click trực tiếp trên danh sách nhóm Công khai hiển thị công cộng
    @Transactional
    public void joinPublicGroupDirectly(String groupId) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        String currentUserId = authentication.getName();

        User currentUser = userRepo.findById(currentUserId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        StudyGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));

        // Nếu cố tình gọi API click tự do này vào một nhóm PRIVATE -> Chặn đứng lại ngay!
        if (group.getVisibility() == GroupVisibility.PRIVATE) {
            throw new AppException(ErrorCode.GROUP_IS_PRIVATE);
        }

        if (memberRepo.existsByGroupAndUser(group, currentUser)) {
            throw new AppException(ErrorCode.USER_ALREADY_IN_GROUP);
        }

        GroupMember member = GroupMember.builder()
                .group(group)
                .user(currentUser)
                .role(GroupRole.MEMBER)
                .build();

        memberRepo.save(member);
    }
}