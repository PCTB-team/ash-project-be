package com.pctb.webapp.repository;

import com.pctb.webapp.entity.User;
import com.pctb.webapp.entity.UserLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserLoginHistoryRepo extends JpaRepository<UserLoginHistory, String> {
    Optional<UserLoginHistory> findByUserAndLoginDate(User user, LocalDate loginDate);

    @Query("select distinct h.loginDate from UserLoginHistory h where h.user = :user order by h.loginDate desc")
    List<LocalDate> findLoginDatesByUserOrderByLoginDateDesc(@Param("user") User user);

    // 🌟 THÊM MỚI (GIỮ NGUYÊN CÁC HÀM CŨ): Phục vụ việc xóa sạch lịch sử đăng nhập trước khi xóa User
    @Modifying
    @Transactional
    @Query("delete from UserLoginHistory h where h.user = :user")
    void deleteByUser(@Param("user") User user);
}