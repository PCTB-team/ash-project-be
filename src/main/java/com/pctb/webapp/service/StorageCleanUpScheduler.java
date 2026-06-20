package com.pctb.webapp.service; // Thay đổi package phù hợp với cấu trúc dự án của bạn

import com.pctb.webapp.entity.User;
import com.pctb.webapp.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class StorageCleanUpScheduler {

    private final UserRepo userRepo;

    /**
     * TỰ ĐỘNG CHẠY VÀO LÚC 00:00:00 MỖI ĐÊM
     * Giải thích biểu thức Cron: "giây phút giờ ngày tháng thứ"
     * "0 0 0 * * ?" nghĩa là Giây=0, Phút=0, Giờ=0 của tất cả các ngày trong tháng.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void cleanExpiredStoragePlans() {
        log.info("[CRON JOB] Bắt đầu quét các tài khoản hết hạn gói dung lượng VIP...");

        LocalDateTime now = LocalDateTime.now();

        // 1. Tìm tất cả User có ngày hết hạn nhỏ hơn thời gian hiện tại
        List<User> expiredUsers = userRepo.findByStorageExpiredAtBefore(now);

        if (expiredUsers.isEmpty()) {
            log.info("[CRON JOB] Không tìm thấy tài khoản nào bị hết hạn gói.");
            return;
        }

        // 2. Duyệt qua từng tài khoản để phạt hạ cấp về gói mặc định
        for (User user : expiredUsers) {
            long defaultQuota = 524288000L; // 500MB quy ra bytes

            user.setStorageQuota(defaultQuota);
            user.setStorageExpiredAt(null); // Reset trạng thái thời hạn về vô hạn (gói cơ bản)

            userRepo.save(user);

            log.warn("[CRON JOB] Tài khoản [{}] đã bị tự động hạ về 500MB do hết hạn gói cước ngày: {}",
                    user.getUsername(), now);
        }

        log.info("[CRON JOB] Đã xử lý hạ cấp xong cho {} tài khoản.", expiredUsers.size());
    }
}