package com.pctb.webapp.repository;

import com.pctb.webapp.entity.StudyGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StudyGroupRepo extends JpaRepository<StudyGroup, String> {
    Optional<StudyGroup> findByInviteToken(String inviteToken);

    boolean existsByInviteToken(String inviteToken);

    List<StudyGroup> findByOwnerIdOrderByCreatedAtDesc(String ownerId);

    @Query(
            value = """
                    SELECT DISTINCT g
                    FROM StudyGroup g
                    LEFT JOIN GroupMember gm ON gm.group = g AND gm.user.id = :userId
                    WHERE (gm.id IS NOT NULL OR g.owner.id = :userId)
                      AND (
                            :keyword IS NULL
                            OR LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(g.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(g.owner.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    ORDER BY COALESCE(gm.joinedAt, g.createdAt) DESC
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT g)
                    FROM StudyGroup g
                    LEFT JOIN GroupMember gm ON gm.group = g AND gm.user.id = :userId
                    WHERE (gm.id IS NOT NULL OR g.owner.id = :userId)
                      AND (
                            :keyword IS NULL
                            OR LOWER(g.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(g.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(g.owner.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    """
    )
    Page<StudyGroup> findMyGroups(
            @Param("userId") String userId,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
