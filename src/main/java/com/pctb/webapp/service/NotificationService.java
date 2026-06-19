package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.MarkNotificationReadRequest;
import com.pctb.webapp.dto.response.NotificationListResponse;
import com.pctb.webapp.dto.response.NotificationResponse;
import com.pctb.webapp.entity.GroupMember;
import com.pctb.webapp.entity.Notification;
import com.pctb.webapp.entity.NotificationType;
import com.pctb.webapp.entity.StudyGroup;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.GroupMemberRepo;
import com.pctb.webapp.repository.NotificationRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotificationService {
    NotificationRepo notificationRepo;

    UserRepo userRepo;

    GroupMemberRepo groupMemberRepo;

    // Tao 1 thong bao cho 1 user nhan.
    public void create(
            User receiver,
            User actor,
            NotificationType type,
            String title,
            String message,
            NotificationType resourceType,
            String resourceId,
            String resourceName,
            String groupId,
            String groupName
    ) {
        if (receiver == null) {
            return;
        }

        Notification notification = Notification.builder()
                .receiverUser(receiver)
                .actorUser(actor)
                .type(type)
                .title(title)
                .message(message)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .resourceName(resourceName)
                .groupId(groupId)
                .groupName(groupName)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepo.save(notification);
    }

    // Gui thong bao cho nhieu thanh vien trong group, co the bo qua actor hoac user khac.
    public void createForGroupMembers(
            StudyGroup group,
            User actor,
            NotificationType type,
            String title,
            String message,
            NotificationType resourceType,
            String resourceId,
            String resourceName,
            Set<String> excludedUserIds
    ) {
        List<GroupMember> members = groupMemberRepo.findByGroupIdOrderByJoinedAtAsc(group.getId());

        for (GroupMember member : members) {
            User receiver = member.getUser();
            if (excludedUserIds != null && excludedUserIds.contains(receiver.getId())) {
                continue;
            }

            create(
                    receiver,
                    actor,
                    type,
                    title,
                    message,
                    resourceType,
                    resourceId,
                    resourceName,
                    group.getId(),
                    group.getName()
            );
        }
    }

    // FE goi API nay de lay danh sach thong bao va so luong chua doc.
    @Transactional(readOnly = true)
    public NotificationListResponse getMyNotifications(
            Boolean read,
            int page,
            int size,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1));
        Page<Notification> notificationPage = read == null
                ? notificationRepo.findByReceiverUserOrderByCreatedAtDesc(currentUser, pageable)
                : notificationRepo.findByReceiverUserAndReadOrderByCreatedAtDesc(currentUser, read, pageable);

        return NotificationListResponse.builder()
                .items(notificationPage.getContent().stream()
                        .map(this::buildResponse)
                        .toList())
                .unreadCount(notificationRepo.countByReceiverUserAndReadFalse(currentUser))
                .page(notificationPage.getNumber())
                .size(notificationPage.getSize())
                .totalElements(notificationPage.getTotalElements())
                .totalPages(notificationPage.getTotalPages())
                .build();
    }

    // Mark 1, nhieu, hoac tat ca thong bao thanh da doc.
    @Transactional
    public NotificationListResponse markAsRead(
            MarkNotificationReadRequest request,
            JwtAuthenticationToken authentication
    ) {
        User currentUser = getCurrentUser(authentication);
        List<Notification> notifications;

        if (request != null && Boolean.TRUE.equals(request.getAll())) {
            notifications = notificationRepo.findByReceiverUserAndReadFalse(currentUser);
        } else {
            if (request == null || request.getNotificationIds() == null || request.getNotificationIds().isEmpty()) {
                throw new AppException(ErrorCode.REQUEST_BODY_INVALID);
            }

            notifications = notificationRepo.findByIdInAndReceiverUser(request.getNotificationIds(), currentUser);
        }

        notifications.forEach(notification -> notification.setRead(true));
        notificationRepo.saveAll(notifications);

        return getMyNotifications(null, 0, 20, authentication);
    }

    private NotificationResponse buildResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .resourceType(notification.getResourceType().name())
                .resourceId(notification.getResourceId())
                .resourceName(notification.getResourceName())
                .groupId(notification.getGroupId())
                .groupName(notification.getGroupName())
                .read(Boolean.TRUE.equals(notification.getRead()))
                .createdAt(notification.getCreatedAt() == null ? null : notification.getCreatedAt().toString())
                .build();
    }

    private User getCurrentUser(JwtAuthenticationToken authentication) {
        return userRepo.findById(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }
}
