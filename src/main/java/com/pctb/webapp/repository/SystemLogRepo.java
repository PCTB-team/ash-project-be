package com.pctb.webapp.repository;

import com.pctb.webapp.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemLogRepo extends JpaRepository<SystemLog, Long> {
    Page<SystemLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<SystemLog> findByActorTypeOrderByCreatedAtDesc(String actorType, Pageable pageable);

    Page<SystemLog> findByActorTypeAndActorIdOrderByCreatedAtDesc(String actorType, String actorId, Pageable pageable);

    Page<SystemLog> findByActorIdOrderByCreatedAtDesc(String actorId, Pageable pageable);

    @Query("""
            select l
            from SystemLog l
            where l.actorType = 'ADMIN'
              and l.actionGroup in (
                    'USER_MANAGEMENT',
                    'DOCUMENT_MANAGEMENT',
                    'GROUP_MANAGEMENT',
                    'SYSTEM_MANAGEMENT'
              )
            order by l.createdAt desc
            """)
    Page<SystemLog> findAdminAuditLogs(Pageable pageable);

    @Query("""
            select l
            from SystemLog l
            where l.actorType = 'ADMIN'
              and l.actorId = :actorId
              and l.actionGroup in (
                    'USER_MANAGEMENT',
                    'DOCUMENT_MANAGEMENT',
                    'GROUP_MANAGEMENT',
                    'SYSTEM_MANAGEMENT'
              )
            order by l.createdAt desc
            """)
    Page<SystemLog> findAdminAuditLogsByActorId(String actorId, Pageable pageable);
}
