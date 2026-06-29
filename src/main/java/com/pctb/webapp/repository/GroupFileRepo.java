package com.pctb.webapp.repository;

import com.pctb.webapp.entity.GroupFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GroupFileRepo extends JpaRepository<GroupFile, String> {
    List<GroupFile> findByGroupIdOrderByUploadedAtDesc(String groupId);

    List<GroupFile> findByGroupIdAndDeletedFalseOrderByUploadedAtDesc(String groupId);

    List<GroupFile> findByGroupIdAndDeletedTrueOrderByDeletedAtDesc(String groupId);

    Optional<GroupFile> findByGroupIdAndFileNameAndDeletedFalse(String groupId, String fileName);

    long countByGroupIdAndDeletedFalse(String groupId);

    long countByGroupIdAndDeletedTrue(String groupId);

    long countByGroupId(String groupId);

    @Modifying
    @Query("DELETE FROM GroupFile gf WHERE gf.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") String groupId);
}
