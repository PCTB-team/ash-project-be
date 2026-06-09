package com.pctb.webapp.repository;

import com.pctb.webapp.entity.GroupMember;
import com.pctb.webapp.entity.StudyGroup;
import com.pctb.webapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepo extends JpaRepository<GroupMember, String> {
    boolean existsByGroupAndUser(StudyGroup group, User user);

    // Tìm kiếm cụ thể bản ghi thành viên dựa vào group và user (dùng cho logic kiểm tra quyền OWNER/ADMIN)
    Optional<GroupMember> findByGroupAndUser(StudyGroup group, User user);

    List<GroupMember> findByGroupOrderByJoinedAtAsc(StudyGroup group);
}
