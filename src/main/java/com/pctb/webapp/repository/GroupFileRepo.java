package com.pctb.webapp.repository;

import com.pctb.webapp.entity.GroupFile;
import com.pctb.webapp.entity.User;
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

    // Tính cả file đang hoạt động và file trong thùng rác vì chúng vẫn còn chiếm bộ nhớ.
    @Query("SELECT COALESCE(SUM(gf.fileSize), 0) FROM GroupFile gf WHERE gf.uploadedBy = :uploader")
    Long sumFileSizeByUploader(@Param("uploader") User uploader);

    @Modifying
    @Query("DELETE FROM GroupFile gf WHERE gf.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") String groupId);
}
