package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.LockUserRequest;
import com.pctb.webapp.dto.response.DashboardStatsResponse;
import com.pctb.webapp.dto.response.UserResponse;
import com.pctb.webapp.dto.response.UserStorageDetailResponse;
import com.pctb.webapp.dto.response.UserStorageResponse;
import com.pctb.webapp.entity.SystemLog;
import com.pctb.webapp.entity.TransactionStatus;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.SystemLogRepo;
import com.pctb.webapp.repository.TransactionRepo;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminService {
    UserRepo userRepo;
    SystemLogRepo systemLogRepo;
    TransactionRepo transactionRepo;

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

    // =========================================================================
    // KHU VỰC BỔ SUNG: ADMIN CHỦ ĐỘNG HỦY GÓI DUNG LƯỢNG VIP CỦA USER
    // =========================================================================
    @Transactional
    public void cancelUserStoragePlan(String userId, String adminUsername) {
        // 1. Tìm kiếm thông tin user theo ID, nếu không thấy ném lỗi 1204
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // 2. Định nghĩa hạn mức mặc định (500MB = 500 * 1024 * 1024 bytes)
        long defaultQuota = 524288000L;

        // Đẩy user về trạng thái gói mặc định ban đầu
        user.setStorageQuota(defaultQuota);
        user.setStorageExpiredAt(null); // Xóa ngày hết hạn vì gói mặc định có thời hạn vĩnh viễn
        userRepo.save(user);

        // 3. Ghi nhật ký hệ thống (System Log)
        SystemLog log = SystemLog.builder()
                .actor(adminUsername)
                .action("CANCEL_USER_PLAN")
                .details("Admin '" + adminUsername + "' đã chủ động hủy gói VIP và đặt lại dung lượng mặc định (500MB) cho tài khoản '" + user.getUsername() + "'")
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

    // Tạo dữ liệu dashboard đầy đủ gồm User, Doanh thu giao dịch và Dung lượng hệ thống / người dùng.
    public DashboardStatsResponse getDashboardStats() {
        LocalDateTime now = LocalDateTime.now();

        // --- 1. THỐNG KÊ TĂNG TRƯỞNG USER ---
        Map<String, Long> growth = new HashMap<>();
        growth.put("last7Days", userRepo.countByCreatedAtAfter(now.minusDays(7)));
        growth.put("last2Weeks", userRepo.countByCreatedAtAfter(now.minusDays(14)));
        growth.put("last1Month", userRepo.countByCreatedAtAfter(now.minusMonths(1)));

        // --- 2. THỐNG KÊ DOANH THU & GIAO DỊCH ---
        Long totalRevenueLong = transactionRepo.sumTotalRevenue();
        double totalRevenue = totalRevenueLong != null ? totalRevenueLong.doubleValue() : 0.0;

        // Đếm số giao dịch thành công thông qua Enum định sẵn
        long totalTransactions = transactionRepo.countByStatus(TransactionStatus.SUCCESS);

        // Map dữ liệu từ List<Object[]> sang Map<String, Double>
        Map<String, Double> revenueByPackage = transactionRepo.getRevenueGroupedByPackageRaw().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0], // Tên gói (planName)
                        row -> {
                            if (row[1] instanceof Long) return ((Long) row[1]).doubleValue();
                            if (row[1] instanceof Double) return (Double) row[1];
                            return 0.0;
                        },
                        (existing, replacement) -> existing
                ));

        // --- 3. THỐNG KÊ DUNG LƯỢNG TỔNG HỆ THỐNG (ADMIN) ---
        long totalAdminUsed = userRepo.sumTotalUsedStorage();
        long totalAdminMax = userRepo.sumTotalMaxStorage();
        long totalAdminRemaining = totalAdminMax - totalAdminUsed;
        double adminUsagePercent = totalAdminMax > 0 ? ((double) totalAdminUsed / totalAdminMax) * 100 : 0;

        UserStorageResponse adminStorageStats = UserStorageResponse.builder()
                .usedStorage(totalAdminUsed)
                .maxStorage(totalAdminMax)
                .remainingStorage(totalAdminRemaining)
                .usagePercent(Math.round(adminUsagePercent * 100.0) / 100.0)
                .build();

        // --- 4. THỐNG KÊ DUNG LƯỢNG NGƯỜI DÙNG CAO NHẤT (TOP 5 + THUMBNAIL) ---
        List<UserStorageDetailResponse> topUsersStorage = userRepo.findTopUsersByStorage(PageRequest.of(0, 5))
                .stream()
                .map(userEntity -> {
                    long used = userEntity.getStorageUsed(); // Đã đồng bộ với Entity User gốc
                    long max = userEntity.getStorageQuota(); // Đã đồng bộ với Entity User gốc
                    double percent = max > 0 ? ((double) used / max) * 100 : 0;

                    return UserStorageDetailResponse.builder()
                            .username(userEntity.getUsername())
                            .fullname(userEntity.getFullname())
                            .thumbnailUrl(userEntity.getAvatarUrl()) // Lấy chính xác avatarUrl từ User
                            .storage(UserStorageResponse.builder()
                                    .usedStorage(used)
                                    .maxStorage(max)
                                    .remainingStorage(max - used)
                                    .usagePercent(Math.round(percent * 100.0) / 100.0)
                                    .build())
                            .build();
                })
                .collect(Collectors.toList());

        // --- 5. ĐÓNG GÓI TRẢ VỀ DỮ LIỆU SẠCH CHO CLIENT ---
        return DashboardStatsResponse.builder()
                .totalUsers(userRepo.count())
                .userGrowth(growth)
                .totalRevenue(totalRevenue)
                .totalTransactions(totalTransactions)
                .revenueByPackage(revenueByPackage)
                .adminStorageStats(adminStorageStats)
                .topUsersStorage(topUsersStorage)
                .build();
    }
}