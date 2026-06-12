package com.pctb.webapp.ai.service;

import com.pctb.webapp.ai.dto.request.CreateAiChatSessionRequest;
import com.pctb.webapp.ai.dto.response.AiChatSessionResponse;
import com.pctb.webapp.ai.entity.AiChatSession;
import com.pctb.webapp.ai.entity.ChatScopeType;
import com.pctb.webapp.ai.repository.AiChatSessionRepo;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Quan ly chat session: tao session, lay danh sach session va verify session thuoc user.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AiChatSessionService {
    AiChatSessionRepo sessionRepo;

    UserRepo userRepo;

    AiPermissionService permissionService;

    /**
     * Tao session moi sau khi validate scope va quyen truy cap cua user.
     */
    @Transactional
    public AiChatSessionResponse createSession(CreateAiChatSessionRequest request, JwtAuthenticationToken authentication) {
        String userId = authentication.getName();
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        validateScope(request, userId);
        LocalDateTime now = LocalDateTime.now();

        AiChatSession session = AiChatSession.builder()
                .owner(user)
                .scopeType(request.getScopeType())
                .documentId(trimToNull(request.getDocumentId()))
                .folderId(trimToNull(request.getFolderId()))
                .groupId(trimToNull(request.getGroupId()))
                .title(buildTitle(request.getScopeType()))
                .createdAt(now)
                .updatedAt(now)
                .deleted(false)
                .build();

        return buildSessionResponse(sessionRepo.save(session));
    }

    /**
     * Lay tat ca session chua deleted cua user dang dang nhap.
     */
    public List<AiChatSessionResponse> getMySessions(JwtAuthenticationToken authentication) {
        return sessionRepo.findByOwnerIdAndDeletedFalseOrderByUpdatedAtDesc(authentication.getName())
                .stream()
                .map(this::buildSessionResponse)
                .toList();
    }

    /**
     * Load session va dam bao session nay thuoc user dang dang nhap.
     * Moi lan gui message hoac lay lich su deu phai qua ham nay.
     */
    public AiChatSession getOwnedActiveSession(String sessionId, String userId) {
        AiChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new AppException(ErrorCode.REQUEST_PARAMETER_INVALID));

        if (!session.getOwner().getId().equals(userId) || Boolean.TRUE.equals(session.getDeleted())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        requireCurrentScopeAccess(session, userId);
        return session;
    }

    /**
     * Map entity AiChatSession sang DTO de tranh tra entity lazy ra controller.
     */
    public AiChatSessionResponse buildSessionResponse(AiChatSession session) {
        return AiChatSessionResponse.builder()
                .sessionId(session.getId())
                .scopeType(session.getScopeType().name())
                .documentId(session.getDocumentId())
                .folderId(session.getFolderId())
                .groupId(session.getGroupId())
                .title(session.getTitle())
                .createdAt(session.getCreatedAt() == null ? null : session.getCreatedAt().toString())
                .updatedAt(session.getUpdatedAt() == null ? null : session.getUpdatedAt().toString())
                .build();
    }

    /**
     * Validate request tao session theo tung scope.
     * Vi du PERSONAL_DOCUMENT bat buoc co documentId va document phai thuoc user.
     */
    private void validateScope(CreateAiChatSessionRequest request, String userId) {
        if (request == null || request.getScopeType() == null) {
            throw new AppException(ErrorCode.REQUEST_BODY_INVALID);
        }

        switch (request.getScopeType()) {
            case PERSONAL_DOCUMENT -> permissionService.requireOwnedActiveDocument(requireText(request.getDocumentId()), userId);
            case PERSONAL_FOLDER -> permissionService.requireOwnedActiveFolder(requireText(request.getFolderId()), userId);
            case PERSONAL_LIBRARY -> {
            }
            case GROUP_DOCUMENTS -> permissionService.requireApprovedGroupMember(requireText(request.getGroupId()), userId);
        }
    }

    /**
     * Kiem tra lai quyen tai thoi diem user dang chat.
     * Neu tai lieu/group bi thay doi quyen sau khi tao session thi van bi chan.
     */
    private void requireCurrentScopeAccess(AiChatSession session, String userId) {
        switch (session.getScopeType()) {
            case PERSONAL_DOCUMENT -> permissionService.requireOwnedActiveDocument(session.getDocumentId(), userId);
            case PERSONAL_FOLDER -> permissionService.requireOwnedActiveFolder(session.getFolderId(), userId);
            case PERSONAL_LIBRARY -> {
            }
            case GROUP_DOCUMENTS -> permissionService.requireApprovedGroupMember(session.getGroupId(), userId);
        }
    }

    /**
     * Bat buoc input text phai co gia tri.
     */
    private String requireText(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new AppException(ErrorCode.REQUEST_BODY_INVALID);
        }

        return normalized;
    }

    /**
     * Chuan hoa chuoi rong/null ve null de validate de hon.
     */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    /**
     * Tao title mac dinh theo scope de FE hien thi tam trong danh sach chat.
     */
    private String buildTitle(ChatScopeType scopeType) {
        return switch (scopeType) {
            case PERSONAL_DOCUMENT -> "Document chat";
            case PERSONAL_FOLDER -> "Folder chat";
            case PERSONAL_LIBRARY -> "Library chat";
            case GROUP_DOCUMENTS -> "Group chat";
        };
    }
}
