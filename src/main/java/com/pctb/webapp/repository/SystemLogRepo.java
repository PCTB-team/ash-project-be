package com.pctb.webapp.repository;

import com.pctb.webapp.entity.SystemLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemLogRepo extends JpaRepository<SystemLog, Long> {
    // Lấy danh sách logs sắp xếp mới nhất lên đầu
    Page<SystemLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}