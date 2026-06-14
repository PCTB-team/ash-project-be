package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.LockUserRequest;
import com.pctb.webapp.dto.response.DashboardStatsResponse;
import com.pctb.webapp.dto.response.UserResponse;
import com.pctb.webapp.entity.SystemLog;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.SystemLogRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminService {
    UserRepo userRepo;
    SystemLogRepo systemLogRepo;

    // Lấy danh sách user có phân trang; nếu có keyword thì tìm theo username, email hoặc fullname.
    public Page<UserResponse> getAllAndSearchUsers(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> usersPage = (keyword == null || keyword.trim().isEmpty())
                ? userRepo.findAll(pageable)
                : userRepo.searchUsers(keyword.trim(), pageable);

        return usersPage.map(this::mapToUserResponse);
    }

    // Khóa tài khoản user, lưu lý do khóa và ghi log hệ thống để admin có lịch sử thao tác.
    @Transactional
    // Thực hiện khóa user trong một transaction để trạng thái khóa và system log được lưu nhất quán.
    public void lockUser(String userId, LockUserRequest request, String adminUsername) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.getUsername().equals(adminUsername)) {
            throw new AppException(ErrorCode.ADMIN_CANNOT_LOCK_SELF);
        }

        user.setAccountNonLocked(false);
        user.setLockedAt(LocalDateTime.now());
        user.setLockedReason(request.getReason());
        user.setLockedByAdmin(adminUsername);
        userRepo.save(user);

        SystemLog log = SystemLog.builder()
                .actor(adminUsername)
                .action("LOCK_USER")
                .details("Đã khóa tài khoản '" + user.getUsername() + "' | Lý do: " + request.getReason())
                .createdAt(LocalDateTime.now())
                .build();
        systemLogRepo.save(log);
    }

    // Mở khóa tài khoản user, xóa thông tin khóa cũ và ghi log thao tác mở khóa.
    @Transactional
    // Thực hiện mở khóa user trong một transaction để cập nhật user và ghi log cùng thành công.
    public void unlockUser(String userId, String adminUsername) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (user.isAccountNonLocked()) {
            throw new AppException(ErrorCode.ACCOUNT_ALREADY_UNLOCKED);
        }

        user.setAccountNonLocked(true);
        user.setLockedAt(null);
        user.setLockedReason(null);
        user.setLockedByAdmin(null);
        userRepo.save(user);

        SystemLog log = SystemLog.builder()
                .actor(adminUsername)
                .action("UNLOCK_USER")
                .details("Đã mở khóa tài khoản '" + user.getUsername() + "'")
                .createdAt(LocalDateTime.now())
                .build();
        systemLogRepo.save(log);
    }

    // Lấy nhật ký hệ thống theo phân trang, sắp xếp theo thứ tự mới nhất ở repository.
    public Page<SystemLog> getSystemLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return systemLogRepo.findAllByOrderByCreatedAtDesc(pageable);
    }

    // Trả về thống kê hệ thống đơn giản gồm tổng số user và thời điểm tạo thống kê.
    public Map<String, Object> getSystemStats() {
        long totalUsers = userRepo.count();
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("timestamp", LocalDateTime.now());
        return stats;
    }

    // Chuyển entity User sang DTO trả về cho màn hình quản trị, chỉ lấy các field cần hiển thị.
    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setFullname(user.getFullname());
        response.setEmail(user.getEmail());
        response.setRoles(user.getRoles());

        response.setAccountNonLocked(user.isAccountNonLocked());
        response.setLockedAt(user.getLockedAt());
        response.setLockedReason(user.getLockedReason());
        response.setLockedByAdmin(user.getLockedByAdmin());

        return response;
    }

    // Tạo dữ liệu dashboard gồm tổng user và số user mới trong 7 ngày, 2 tuần, 1 tháng gần nhất.
    public DashboardStatsResponse getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();

        Map<String, Long> growth = new HashMap<>();
        growth.put("last7Days", userRepo.countByCreatedAtAfter(now.minusDays(7)));
        growth.put("last2Weeks", userRepo.countByCreatedAtAfter(now.minusDays(14)));
        growth.put("last1Month", userRepo.countByCreatedAtAfter(now.minusMonths(1)));

        return DashboardStatsResponse.builder()
                .totalUsers(userRepo.count())
                .userGrowth(growth)
                .build();
    }
}
