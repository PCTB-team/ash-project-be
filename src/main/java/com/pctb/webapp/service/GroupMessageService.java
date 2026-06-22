package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.SendGroupMessageRequest;
import com.pctb.webapp.dto.response.GroupMessagePageResponse;
import com.pctb.webapp.dto.response.GroupMessageResponse;
import com.pctb.webapp.entity.GroupMember;
import com.pctb.webapp.entity.GroupMessage;
import com.pctb.webapp.entity.GroupRole;
import com.pctb.webapp.entity.StudyGroup;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.GroupMemberRepo;
import com.pctb.webapp.repository.GroupMessageRepo;
import com.pctb.webapp.repository.StudyGroupRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupMessageService {
    static final int MAX_MESSAGE_LENGTH = 1000;

    GroupMessageRepo groupMessageRepo;

    GroupMemberRepo groupMemberRepo;

    StudyGroupRepo studyGroupRepo;

    UserRepo userRepo;

    SimpMessagingTemplate messagingTemplate;

    /**
     * Lay lich su chat cua group co phan trang.
     */
    @Transactional(readOnly = true)
    public GroupMessagePageResponse getMessages(
            String groupId,
            int page,
            int size,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        StudyGroup group = requireGroupMember(groupId, currentUser).getGroup();
        Pageable pageable = PageRequest.of(normalizePage(page), normalizePageSize(size));
        Page<GroupMessage> messagePage = groupMessageRepo.findByGroupIdOrderByCreatedAtDesc(group.getId(), pageable);
        List<GroupMessageResponse> messages = messagePage.getContent().stream()
                .map(this::buildGroupMessageResponse)
                .toList();

        return GroupMessagePageResponse.builder()
                .items(messages)
                .page(messagePage.getNumber())
                .size(messagePage.getSize())
                .totalElements(messagePage.getTotalElements())
                .totalPages(messagePage.getTotalPages())
                .build();
    }

    /**
     * Gui tin nhan moi vao group.
     */
    @Transactional
    public GroupMessageResponse sendMessage(
            String groupId,
            SendGroupMessageRequest request,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        GroupMember member = requireGroupMember(groupId, currentUser);
        ensureCanChat(member);
        StudyGroup group = member.getGroup();
        String content = normalizeMessageContent(request == null ? null : request.getContent());

        GroupMessage message = GroupMessage.builder()
                .group(group)
                .sender(currentUser)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();

        GroupMessageResponse response = buildGroupMessageResponse(groupMessageRepo.save(message));
        publishMessage(group.getId(), response);

        return response;
    }

    private void publishMessage(String groupId, GroupMessageResponse response) {
        messagingTemplate.convertAndSend(
                "/topic/groups/" + groupId + "/messages",
                response
        );
    }

    private User getCurrentUser(JwtAuthenticationToken authentication) {
        return userRepo.findById(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    private GroupMember requireGroupMember(String groupId, User currentUser) {
        String normalizedGroupId = normalizeRequiredText(groupId);
        GroupMember member = groupMemberRepo.findByGroupIdAndUserId(normalizedGroupId, currentUser.getId())
                .orElse(null);

        if (member != null) {
            return member;
        }

        StudyGroup group = studyGroupRepo.findById(normalizedGroupId)
                .orElseThrow(() -> new AppException(ErrorCode.GROUP_NOT_FOUND));
        if (group.getOwner().getId().equals(currentUser.getId())) {
            return groupMemberRepo.save(GroupMember.builder()
                    .group(group)
                    .user(currentUser)
                    .role(GroupRole.LEADER)
                    .canUpload(true)
                    .canChat(true)
                    .joinedAt(LocalDateTime.now())
                    .build());
        }

        throw new AppException(ErrorCode.GROUP_ACCESS_DENIED);
    }

    private void ensureCanChat(GroupMember member) {
        if (member.getRole() == GroupRole.LEADER) {
            return;
        }

        if (!Boolean.TRUE.equals(member.getCanChat())) {
            throw new AppException(ErrorCode.GROUP_CHAT_NOT_ALLOWED);
        }
    }

    private GroupMessageResponse buildGroupMessageResponse(GroupMessage message) {
        User sender = message.getSender();
        StudyGroup group = message.getGroup();

        return GroupMessageResponse.builder()
                .messageId(message.getId())
                .groupId(group.getId())
                .senderId(sender.getId())
                .senderName(sender.getFullname())
                .senderAvatarUrl(sender.getAvatarUrl())
                .content(message.getContent())
                .createdAt(message.getCreatedAt() == null ? null : message.getCreatedAt().toString())
                .build();
    }

    private String normalizeMessageContent(String content) {
        String normalizedContent = normalizeOptionalText(content);
        if (normalizedContent == null) {
            throw new AppException(ErrorCode.GROUP_MESSAGE_EMPTY);
        }

        if (normalizedContent.length() > MAX_MESSAGE_LENGTH) {
            throw new AppException(ErrorCode.GROUP_MESSAGE_TOO_LONG);
        }

        return normalizedContent;
    }

    private String normalizeRequiredText(String value) {
        String normalizedValue = normalizeOptionalText(value);
        if (normalizedValue == null) {
            throw new AppException(ErrorCode.REQUEST_PARAMETER_INVALID);
        }

        return normalizedValue;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String cleanValue = value.trim();
        return cleanValue.isBlank() ? null : cleanValue;
    }

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizePageSize(int size) {
        if (size < 1) {
            return 30;
        }

        return Math.min(size, 100);
    }
}
