package com.pctb.webapp.repository;

import com.pctb.webapp.entity.GroupFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GroupFileRepo extends JpaRepository<GroupFile, String> {
    List<GroupFile> findByGroupIdOrderByUploadedAtDesc(String groupId);

    List<GroupFile> findByGroupIdAndDeletedFalseOrderByUploadedAtDesc(String groupId);

    List<GroupFile> findByGroupIdAndDeletedTrueOrderByDeletedAtDesc(String groupId);

    long countByGroupIdAndDeletedFalse(String groupId);

    long countByGroupIdAndDeletedTrue(String groupId);
}
