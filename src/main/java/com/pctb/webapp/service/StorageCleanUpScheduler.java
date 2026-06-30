package com.pctb.webapp.service;

import com.pctb.webapp.entity.User;
import com.pctb.webapp.entity.UserLoginHistory;
import com.pctb.webapp.repository.UserLoginHistoryRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@EnableScheduling
@Slf4j
public class StorageCleanUpScheduler {

    private final UserRepo userRepo;
    private final UserLoginHistoryRepo userLoginHistoryRepo;
    private final RedisService redisService;

    @Value("${app.admin.inactive-lock-days:60}")
    private long inactiveLockDays;

    // Tự động kích hoạt quét ngầm vào lúc 00:00 hàng đêm (Báo hiệu chu kỳ ngày mới)
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void scanAndDowngradeExpiredUsers() {
        log.info("=== STARTING AUTOMATIC STORAGE EXPIRED CHECK SCHEDULER ===");

        // Tìm toàn bộ User có thiết lập ngày hết hạn và ngày hết hạn nhỏ hơn thời điểm hiện tại
        List<User> expiredUsers = userRepo.findAll().stream()
                .filter(u -> u.getStorageExpiredAt() != null && u.getStorageExpiredAt().isBefore(LocalDateTime.now()))
                .toList();

        if (expiredUsers.isEmpty()) {
            log.info("No expired VIP accounts found today.");
            return;
        }

        for (User user : expiredUsers) {
            log.warn("Account [{}] has expired on {}. System automatically downgrades quota to 500MB.",
                    user.getUsername(), user.getStorageExpiredAt());

            // Hạ cấp cước phạt: Thu hồi hạn mức bộ nhớ về 500MB cơ bản
            user.setStorageQuota(524288000L);
            user.setStorageExpiredAt(null); // Trở về trạng thái dùng vĩnh viễn mức cơ bản
            userRepo.save(user);
        }

        log.info("=== AUTOMATIC STORAGE EXPIRED CHECK COMPLETED. {} USERS DOWNGRADED ===", expiredUsers.size());
    }

    @Scheduled(cron = "0 15 0 * * ?")
    @Transactional
    public void scanAndLockInactiveUsers() {
        log.info("=== STARTING AUTOMATIC INACTIVE ACCOUNT LOCK SCHEDULER ===");

        LocalDate cutoffDate = LocalDate.now().minusDays(inactiveLockDays);
        List<User> inactiveUsers = userRepo.findAll().stream()
                .filter(User::isAccountNonLocked)
                .filter(user -> !isAdmin(user))
                .filter(user -> !resolveLastActivityDate(user).isAfter(cutoffDate))
                .toList();

        for (User user : inactiveUsers) {
            user.setAccountNonLocked(false);
            user.setLockedAt(LocalDateTime.now());
            user.setLockedReason("Automatically locked due to inactivity for " + inactiveLockDays + " days");
            user.setLockedByAdmin("SYSTEM");
            userRepo.save(user);
            redisService.delete(refreshTokenKey(user.getId()));
        }

        log.info("=== AUTOMATIC INACTIVE ACCOUNT LOCK COMPLETED. {} USERS LOCKED ===", inactiveUsers.size());
    }

    private LocalDate resolveLastActivityDate(User user) {
        return userLoginHistoryRepo.findTopByUserOrderByLoginDateDesc(user)
                .map(UserLoginHistory::getLoginDate)
                .orElseGet(() -> user.getCreatedAt() != null ? user.getCreatedAt().toLocalDate() : LocalDate.now());
    }

    private boolean isAdmin(User user) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getName()));
    }

    private String refreshTokenKey(String userId) {
        return "auth:refresh:" + userId;
    }
}
