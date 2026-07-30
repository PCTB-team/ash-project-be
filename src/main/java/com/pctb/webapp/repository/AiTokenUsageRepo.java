package com.pctb.webapp.repository;

import com.pctb.webapp.entity.AiTokenUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AiTokenUsageRepo extends JpaRepository<AiTokenUsage, String> {
    long countByUser_IdAndCreatedAtGreaterThanEqual(String userId, LocalDateTime from);

    @Query("""
            SELECT COALESCE(SUM(u.totalTokens), 0)
            FROM AiTokenUsage u
            WHERE u.user.id = :userId
              AND u.createdAt >= :from
            """)
    long sumTotalTokensByUserIdSince(
            @Param("userId") String userId,
            @Param("from") LocalDateTime from
    );

    long countByCreatedAtGreaterThanEqual(LocalDateTime from);

    @Query("""
            SELECT COALESCE(SUM(u.totalTokens), 0)
            FROM AiTokenUsage u
            WHERE u.createdAt >= :from
            """)
    long sumTotalTokensSince(@Param("from") LocalDateTime from);
}
