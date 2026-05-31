package com.pctb.webapp.repository;

import com.pctb.webapp.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, String> {
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);


}
