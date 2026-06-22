package com.pctb.webapp.repository;

import com.pctb.webapp.entity.GroupMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupMessageRepo extends JpaRepository<GroupMessage, String> {
    Page<GroupMessage> findByGroupIdOrderByCreatedAtDesc(String groupId, Pageable pageable);
}
