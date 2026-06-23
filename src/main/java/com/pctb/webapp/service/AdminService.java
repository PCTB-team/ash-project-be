package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.LockUserRequest;
import com.pctb.webapp.dto.response.AdminTransactionResponse;
import com.pctb.webapp.dto.response.LoginLogResponse;
import com.pctb.webapp.dto.response.UserResponse;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.mapper.UserMapper;
import com.pctb.webapp.repository.TransactionRepo;
import com.pctb.webapp.repository.UserRepo;
import com.pctb.webapp.repository.UserLoginHistoryRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminService {

    UserRepo userRepo;
    TransactionRepo transactionRepo;
    UserLoginHistoryRepo userLoginHistoryRepo;
    UserMapper userMapper;

    // 🔍 1. TÌM KIẾM USER TRÊN TABLE (Đã sắp xếp tài khoản mới lên đầu)
    public List<UserResponse> searchUsers(String keyword) {
        if (keyword == null || keyword.isBlank()
                || "null".equalsIgnoreCase(keyword.trim())
                || "undefined".equalsIgnoreCase(keyword.trim())) {
            return userRepo.findAll().stream()
                    .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .map(userMapper::toUserResponse)
                    .collect(Collectors.toList());
        }

        String cleanKeyword = keyword.trim().toLowerCase();
        return userRepo.findAll().stream()
                .filter(user -> (user.getUsername() != null && user.getUsername().toLowerCase().contains(cleanKeyword))
                        || (user.getFullname() != null && user.getFullname().toLowerCase().contains(cleanKeyword))
                        || (user.getEmail() != null && user.getEmail().toLowerCase().contains(cleanKeyword)))
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    // 📜 2. CHECK LOG GIAO DỊCH DÒNG TIỀN (Sắp xếp hóa đơn mới nhất lên đầu bảng)
    public List<AdminTransactionResponse> getTransactionLogs(String status) {
        return transactionRepo.findAll().stream()
                .filter(tx -> status == null || status.isBlank() || tx.getStatus().name().equalsIgnoreCase(status.trim()))
                .sorted(Comparator.comparing(com.pctb.webapp.entity.Transaction::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(tx -> AdminTransactionResponse.builder()
                        .transactionId(tx.getId())
                        .orderCode(tx.getOrderCode())
                        .username(tx.getUser().getUsername())
                        .email(tx.getUser().getEmail())
                        .planName(tx.getPlan().getPlanName())
                        .amount(tx.getAmount())
                        .status(tx.getStatus().name())
                        .createdAt(tx.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // 🔒 3. NÚT HÀNH ĐỘNG: Khóa tài khoản người dùng vi phạm
    @Transactional
    public void lockUser(String userId, LockUserRequest request, String adminUsername) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setAccountNonLocked(false);
        user.setLockedAt(LocalDateTime.now());
        user.setLockedReason(request.getReason());
        user.setLockedByAdmin(adminUsername);

        userRepo.save(user);
    }

    // 🔓 4. NÚT HÀNH ĐỘNG: Mở khóa tài khoản người dùng
    @Transactional
    public void unlockUser(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setAccountNonLocked(true);
        user.setLockedAt(null);
        user.setLockedReason(null);
        user.setLockedByAdmin(null);

        userRepo.save(user);
    }

    // ❌ 5. NÚT HÀNH ĐỘNG: Hạ cấp / Hủy VIP thủ công (Đưa về 500MB)
    @Transactional
    public void cancelUserVipPlan(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        user.setStorageQuota(524288000L);
        user.setStorageExpiredAt(null);

        userRepo.save(user);
    }

    // 🔐 6. HÀM LOG BẢO MẬT CHUẨN: Lọc loại log kết hợp tìm kiếm ký tự liên quan
    public List<UserResponse> getAccountAuditLogs(String type, String keyword) {
        List<User> allUsers = userRepo.findAll();
        java.util.stream.Stream<User> userStream = allUsers.stream();

        if ("LOCKED".equalsIgnoreCase(type)) {
            userStream = userStream.filter(user -> !user.isAccountNonLocked());
        } else if ("NEW".equalsIgnoreCase(type)) {
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            userStream = userStream.filter(user -> user.getCreatedAt() != null && user.getCreatedAt().isAfter(sevenDaysAgo));
        }

        if (keyword != null && !keyword.isBlank()) {
            String cleanKeyword = keyword.trim().toLowerCase();
            userStream = userStream.filter(user ->
                    (user.getUsername() != null && user.getUsername().toLowerCase().contains(cleanKeyword))
                            || (user.getFullname() != null && user.getFullname().toLowerCase().contains(cleanKeyword))
                            || (user.getEmail() != null && user.getEmail().toLowerCase().contains(cleanKeyword))
            );
        }

        return userStream
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(userMapper::toUserResponse)
                .collect(Collectors.toList());
    }

    // 🌐 7. ĐÃ ĐỒNG BỘ: Không còn lỗi ép kiểu, bảo mật dữ liệu tuyệt đối (Sắp xếp mới nhất lên đầu)
    public List<LoginLogResponse> getLoginLogs() {
        return userLoginHistoryRepo.findAll().stream()
                .sorted((log1, log2) -> {
                    if (log1.getLoginDate() == null) return 1;
                    if (log2.getLoginDate() == null) return -1;
                    return log2.getLoginDate().compareTo(log1.getLoginDate()); // Đảo log mới nhất lên đầu bảng
                })
                .map(log -> {
                    String uName = (log.getUser() != null) ? log.getUser().getUsername() : "N/A";
                    String uEmail = (log.getUser() != null) ? log.getUser().getEmail() : "N/A";

                    return LoginLogResponse.builder()
                            .id(log.getId())
                            .username(uName)
                            .email(uEmail)
                            .loginDate(log.getLoginDate()) // 🟢 Khớp chuẩn kiểu dữ liệu LocalDate
                            .build();
                })
                .collect(Collectors.toList());
    }
}