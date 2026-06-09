package com.pctb.webapp.repository;

import com.pctb.webapp.entity.StudyGroup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudyGroupRepo extends JpaRepository<StudyGroup, String> {
    Optional<StudyGroup> findByInviteToken(String inviteToken);

    boolean existsByInviteToken(String inviteToken);

    boolean existsByJoinCode(String joinCode);
}
