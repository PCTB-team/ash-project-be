package com.pctb.webapp.repository;

import com.pctb.webapp.entity.GroupMember;
import com.pctb.webapp.entity.StudyGroup;
import com.pctb.webapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepo extends JpaRepository<GroupMember, String> {
    Optional<GroupMember> findByGroupAndUser(StudyGroup group, User user);

    Optional<GroupMember> findByGroupIdAndUserId(String groupId, String userId);

    List<GroupMember> findByGroupIdOrderByJoinedAtAsc(String groupId);

    long countByGroupId(String groupId);
}
