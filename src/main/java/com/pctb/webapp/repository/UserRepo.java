package com.pctb.webapp.repository;

import com.pctb.webapp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepo extends JpaRepository<User, String> {
    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailOrUsername(String email, String username);

    // Tìm kiếm User nâng cao kèm phân trang (Tránh sập hệ thống khi dữ liệu lớn)
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " + //tìm user bằng username
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +   //tìm user bằng email
            "LOWER(u.fullname) LIKE LOWER(CONCAT('%', :keyword, '%'))")     //tìm user bằng fullname
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

}
