package com.pctb.webapp.repository;

import com.pctb.webapp.entity.GroupMember;
import com.pctb.webapp.entity.StudyGroup;
import com.pctb.webapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepo extends JpaRepository<GroupMember, String> {
    Optional<GroupMember> findByGroupAndUser(StudyGroup group, User user);
    Optional<GroupMember> findByGroupIdAndUserId(String groupId, String userId);
    List<GroupMember> findByUserIdOrderByJoinedAtDesc(String userId);
    List<GroupMember> findByGroupIdOrderByJoinedAtAsc(String groupId);
    long countByGroupId(String groupId);

    // THÊM HÀM NÀY ĐỂ GIẢI QUYẾT LỖI
    @Modifying
    @Transactional
    @Query("DELETE FROM GroupMember gm WHERE gm.group.id = :groupId")
    void deleteByGroupId(@Param("groupId") String groupId);
}