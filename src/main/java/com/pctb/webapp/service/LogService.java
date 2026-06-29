package com.pctb.webapp.service;

import com.pctb.webapp.entity.SystemLog;
import com.pctb.webapp.repository.SystemLogRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LogService {
    private final SystemLogRepo systemLogRepo;

    public void log(String actor, String actorType, String action, String targetId, String details) {
        SystemLog log = SystemLog.builder()
                .actor(actor)
                .actorType(actorType)
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

    // THÊM HÀM NÀY ĐỂ GIẢI QUYẾT LỖI
    public void logAction(String actor, String action, String targetId, String details) {
        SystemLog log = SystemLog.builder()
                .actor(actor)
                .actorType(resolveActorType(action))
                .action(action)
                .targetId(targetId)
                .details(details)
                .createdAt(LocalDateTime.now())
                .build();
        systemLogRepo.save(log);
    }

    private String resolveActorType(String action) {
        if (action == null || action.isBlank()) {
            return "USER_ACTION";
        }

        String normalizedAction = action.trim().toUpperCase();
        if (normalizedAction.contains("DOC") || normalizedAction.contains("DOCUMENT")) {
            return "DOCUMENT_LOG";
        }
        if (normalizedAction.contains("ADMIN")
                || normalizedAction.contains("ROLE")
                || normalizedAction.contains("USER")
                || normalizedAction.contains("GROUP")
                || normalizedAction.contains("BAN")
                || normalizedAction.contains("UNLOCK")
                || normalizedAction.contains("DELETE")) {
            return "ADMIN_ACTION";
        }
        return "USER_ACTION";
    }
}
