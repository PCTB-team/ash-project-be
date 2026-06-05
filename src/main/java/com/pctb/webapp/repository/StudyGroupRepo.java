package com.pctb.webapp.repository;

import com.pctb.webapp.entity.StudyGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface StudyGroupRepo extends JpaRepository<StudyGroup, String> {
    Optional<StudyGroup> findByJoinCode(String joinCode);
    boolean existsByJoinCode(String joinCode);
}