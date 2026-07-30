package com.pctb.webapp.service;

import com.pctb.webapp.dto.response.AiQuotaResponse;
import com.pctb.webapp.entity.AiTokenUsage;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.AiTokenUsageRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AiQuotaService {
    final AiTokenUsageRepo aiTokenUsageRepo;
    final UserRepo userRepo;

    @Value("${app.ai.user-daily-request-limit:5}")
    long userDailyRequestLimit;

    @Value("${app.ai.user-daily-token-limit:30000}")
    long userDailyTokenLimit;

    @Value("${app.ai.system-daily-request-limit:18}")
    long systemDailyRequestLimit;

    @Value("${app.ai.system-daily-token-limit:200000}")
    long systemDailyTokenLimit;

    @Transactional(readOnly = true)
    public void validateCanChat(String userId) {
        AiQuotaResponse quota = getQuota(userId);
        if (!Boolean.TRUE.equals(quota.getCanChat())) {
            throw new AppException(ErrorCode.AI_QUOTA_EXCEEDED);
        }
    }

    @Transactional
    public void recordUsage(
            String userId,
            String model,
            String feature,
            int promptTokens,
            int outputTokens,
            int totalTokens
    ) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        aiTokenUsageRepo.save(AiTokenUsage.builder()
                .user(user)
                .model(model)
                .feature(feature)
                .promptTokens(Math.max(promptTokens, 0))
                .outputTokens(Math.max(outputTokens, 0))
                .totalTokens(Math.max(totalTokens, 0))
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional(readOnly = true)
    public AiQuotaResponse getQuota(String userId) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        long requestUsed = aiTokenUsageRepo.countByUser_IdAndCreatedAtGreaterThanEqual(userId, startOfToday);
        long tokenUsed = aiTokenUsageRepo.sumTotalTokensByUserIdSince(userId, startOfToday);
        long systemRequestUsed = aiTokenUsageRepo.countByCreatedAtGreaterThanEqual(startOfToday);
        long systemTokenUsed = aiTokenUsageRepo.sumTotalTokensSince(startOfToday);

        boolean canChat = requestUsed < userDailyRequestLimit
                && tokenUsed < userDailyTokenLimit
                && systemRequestUsed < systemDailyRequestLimit
                && systemTokenUsed < systemDailyTokenLimit;

        return AiQuotaResponse.builder()
                .requestUsedToday(requestUsed)
                .requestLimitPerDay(userDailyRequestLimit)
                .requestRemainingToday(remaining(userDailyRequestLimit, requestUsed))
                .requestUsagePercent(percent(requestUsed, userDailyRequestLimit))
                .tokenUsedToday(tokenUsed)
                .tokenLimitPerDay(userDailyTokenLimit)
                .tokenRemainingToday(remaining(userDailyTokenLimit, tokenUsed))
                .tokenUsagePercent(percent(tokenUsed, userDailyTokenLimit))
                .systemRequestUsedToday(systemRequestUsed)
                .systemRequestLimitPerDay(systemDailyRequestLimit)
                .systemRequestRemainingToday(remaining(systemDailyRequestLimit, systemRequestUsed))
                .systemRequestUsagePercent(percent(systemRequestUsed, systemDailyRequestLimit))
                .systemTokenUsedToday(systemTokenUsed)
                .systemTokenLimitPerDay(systemDailyTokenLimit)
                .systemTokenRemainingToday(remaining(systemDailyTokenLimit, systemTokenUsed))
                .systemTokenUsagePercent(percent(systemTokenUsed, systemDailyTokenLimit))
                .canChat(canChat)
                .build();
    }

    private long remaining(long limit, long used) {
        return Math.max(limit - used, 0);
    }

    private double percent(long used, long limit) {
        if (limit <= 0) {
            return 100;
        }

        return Math.min(100, used * 100.0 / limit);
    }
}
