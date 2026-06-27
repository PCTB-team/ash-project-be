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
}