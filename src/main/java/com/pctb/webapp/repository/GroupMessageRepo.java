package com.pctb.webapp.repository;

import com.pctb.webapp.entity.GroupMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupMessageRepo extends JpaRepository<GroupMessage, String> {
    Page<GroupMessage> findByGroupIdOrderByCreatedAtDesc(String groupId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM GroupMessage gm WHERE gm.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") String groupId);
}
