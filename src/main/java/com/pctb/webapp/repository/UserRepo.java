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
import java.util.List;

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

    // Đếm user tạo sau một thời điểm nhất định
    long countByCreatedAtAfter(LocalDateTime startDateTime);

    // --- ĐÃ ĐỒNG BỘ: TRÁNH CONFLICT TÊN FIELD DUNG LƯỢNG ---

    // 1. Tính tổng dung lượng ĐÃ DÙNG toàn hệ thống (Dùng storageUsed gốc)
    @Query("SELECT COALESCE(SUM(u.storageUsed), 0) FROM User u")
    long sumTotalUsedStorage();

    // 2. Tính tổng dung lượng CẤU HÌNH TỐI ĐA toàn hệ thống (Dùng storageQuota gốc)
    @Query("SELECT COALESCE(SUM(u.storageQuota), 0) FROM User u")
    long sumTotalMaxStorage();

    // 3. Lấy danh sách top người dùng tiêu tốn dung lượng nhất hệ thống
    @Query("SELECT u FROM User u ORDER BY u.storageUsed DESC")
    List<User> findTopUsersByStorage(Pageable pageable);

    // Thêm hàm này để Spring Data JPA tự động tạo câu lệnh SQL tìm kiếm các user hết hạn
    List<User> findByStorageExpiredAtBefore(LocalDateTime dateTime);

}
