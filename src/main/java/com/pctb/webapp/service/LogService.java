package com.pctb.webapp.service;

import com.pctb.webapp.entity.Role;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.entity.SystemLog;
import com.pctb.webapp.repository.SystemLogRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class LogService {
    public static final String ACTOR_ADMIN = "ADMIN";
    public static final String ACTOR_USER = "USER";
    public static final String ACTOR_DOCUMENT = "DOCUMENT";

    public static final String USER_MANAGEMENT = "USER_MANAGEMENT";
    public static final String DOCUMENT_MANAGEMENT = "DOCUMENT_MANAGEMENT";
    public static final String GROUP_MANAGEMENT = "GROUP_MANAGEMENT";
    public static final String SYSTEM_MANAGEMENT = "SYSTEM_MANAGEMENT";

    public static final String ACTION_BAN_USER = "BAN_USER";
    public static final String ACTION_UNBAN_USER = "UNBAN_USER";
    public static final String ACTION_LOCK_USER = "LOCK_USER";
    public static final String ACTION_DELETE_USER = "DELETE_USER";
    public static final String ACTION_UPDATE_ROLE = "UPDATE_ROLE";

    public static final String ACTION_DELETE_DOCUMENT = "DELETE_DOCUMENT";
    public static final String ACTION_APPROVE_DOCUMENT = "APPROVE_DOCUMENT";
    public static final String ACTION_REJECT_DOCUMENT = "REJECT_DOCUMENT";

    public static final String ACTION_LOCK_GROUP = "LOCK_GROUP";
    public static final String ACTION_UNLOCK_GROUP = "UNLOCK_GROUP";
    public static final String ACTION_DELETE_GROUP = "DELETE_GROUP";

    public static final String ACTION_ADMIN_LOGIN = "ADMIN_LOGIN";
    public static final String ACTION_ADMIN_LOGOUT = "ADMIN_LOGOUT";
    public static final String ACTION_UPDATE_SETTINGS = "UPDATE_SETTINGS";

    private final SystemLogRepo systemLogRepo;
    private final UserRepo userRepo;

    public void log(String actor, String actorType, String action, String targetId, String details) {
        SystemLog log = SystemLog.builder()
                .actor(actor)
                .actorType(normalizeActorType(actorType))
                .actionGroup(resolveNonAdminActionGroup(actorType))
                .action(action)
                .targetId(targetId)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build();
        systemLogRepo.save(log);
    }

    public void logAction(String actor, String action, String details) {
        logAction(actor, action, null, details);
    }

    public void logAction(String actor, String action, String targetId, String details) {
        log(actor, ACTOR_USER, action, targetId, details);
    }

    public void logAdminAction(User actor, String actionGroup, String action, String targetId, String details) {
        if (!isAdmin(actor)) {
            return;
        }
        SystemLog log = SystemLog.builder()
                .actorId(actor.getId())
                .actor(displayName(actor))
                .actorType(ACTOR_ADMIN)
                .actionGroup(actionGroup)
                .action(action)
                .targetId(targetId)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build();
        systemLogRepo.save(log);
    }

    public void logAdminAction(String actorId, String actionGroup, String action, String targetId, String details) {
        if (actorId == null || actorId.isBlank()) {
            return;
        }
        userRepo.findById(actorId.trim())
                .ifPresent(actor -> logAdminAction(actor, actionGroup, action, targetId, details));
    }

    public boolean isAdmin(User user) {
        return user != null
                && user.getRoles() != null
                && user.getRoles().stream()
                .map(Role::getName)
                .filter(name -> name != null && !name.isBlank())
                .map(name -> name.trim().toUpperCase(Locale.ROOT))
                .anyMatch(name -> "ADMIN".equals(name) || "ROLE_ADMIN".equals(name));
    }

    private String normalizeActorType(String actorType) {
        if (actorType == null || actorType.isBlank()) {
            return ACTOR_USER;
        }
        String normalized = actorType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "ADMIN_ACTION", "ADMIN_LOG" -> ACTOR_ADMIN;
            case "DOCUMENT_LOG", "DOCUMENT_ACTION" -> ACTOR_DOCUMENT;
            case "USER_ACTION", "USER_LOG" -> ACTOR_USER;
            default -> normalized;
        };
    }

    private String resolveNonAdminActionGroup(String actorType) {
        String normalized = normalizeActorType(actorType);
        if (ACTOR_DOCUMENT.equals(normalized)) {
            return "DOCUMENT_ACTIVITY";
        }
        return "USER_ACTIVITY";
    }

    private String displayName(User user) {
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getId();
    }
}
