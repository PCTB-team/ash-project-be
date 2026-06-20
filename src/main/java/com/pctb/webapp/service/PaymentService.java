package com.pctb.webapp.service;

import com.pctb.webapp.entity.StoragePlan;
import com.pctb.webapp.entity.Transaction;
import com.pctb.webapp.entity.TransactionStatus;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.StoragePlanRepo;
import com.pctb.webapp.repository.TransactionRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepo transactionRepo;
    private final UserRepo userRepo;
    private final StoragePlanRepo storagePlanRepo;

    // =========================
    // CREATE PAYMENT INTENT (ĐÃ THAY ĐỔI LOGIC CHẶN)
    // =========================
    @Transactional
    public Transaction createPaymentIntent(String userId, String planId, String idempotencyKey) {

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        validateUserRole(user);

        if (transactionRepo.existsByIdempotencyKey(idempotencyKey)) {
            throw new AppException(ErrorCode.DUPLICATE_TRANSACTION);
        }

        StoragePlan plan = storagePlanRepo.findById(planId)
                .orElseThrow(() -> new AppException(ErrorCode.PLAN_NOT_FOUND));

        // KHỐNG CHẾ BẬC THANG: Chặn nếu dung lượng gói mới nhỏ hơn hoặc bằng dung lượng hiện tại
        long currentQuota = user.getStorageQuota() == null ? 0L : user.getStorageQuota();
        if (plan.getQuotaSize() <= currentQuota) {
            throw new AppException(ErrorCode.PLAN_LEVEL_LOW);
        }

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .orderCode(System.currentTimeMillis())
                .user(user)
                .plan(plan)
                .amount(plan.getPrice().longValue())
                .quotaAdded(plan.getQuotaSize()) // Lưu hạn mức mới của gói vào đây
                .status(TransactionStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .build();

        return transactionRepo.save(transaction);
    }

    // =========================================================================
    // SUCCESS PAYMENT (ĐÃ CẬP NHẬT: THAY THẾ DUNG LƯỢNG & TÍNH THỜI HẠN VIP)
    // =========================================================================
    @Transactional
    public void processSuccessfulPayment(String transactionId) {
        // 1. Lấy transaction ra và check trạng thái điều hướng chống cộng trùng
        Transaction tx = transactionRepo.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (tx.getStatus() == TransactionStatus.SUCCESS) return;

        // 2. Cập nhật status hóa đơn
        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setUpdatedAt(LocalDateTime.now());
        transactionRepo.save(tx);

        // 3. CẬP NHẬT HẠN MỨC DUNG LƯỢNG MỚI CHO USER
        User user = tx.getUser();
        StoragePlan plan = tx.getPlan(); // Lấy thông tin gói từ hóa đơn

        // Gán thẳng hạn mức mới từ gói cước (đè dung lượng cũ để khống chế 25GB Cloudinary)
        long newQuota = tx.getQuotaAdded();
        user.setStorageQuota(newQuota);

        // 4. TÍNH TOÁN THỜI GIAN HẾT HẠN (MỚI BỔ SUNG ĐỒNG BỘ THÁNG/NĂM)
        int monthsToAdd = plan.getDurationMonths() != null ? plan.getDurationMonths() : 1;
        LocalDateTime currentExpiredAt = user.getStorageExpiredAt();
        LocalDateTime newExpiredAt;

        // Nếu chưa từng mua gói hoặc gói cũ đã hết hạn từ trước -> Tính từ thời điểm hiện tại
        if (currentExpiredAt == null || currentExpiredAt.isBefore(LocalDateTime.now())) {
            newExpiredAt = LocalDateTime.now().plusMonths(monthsToAdd);
        } else {
            // Nếu gói cũ vẫn còn hạn -> Cộng nối tiếp chu kỳ thời gian sử dụng vào ngày cũ (Gia hạn)
            newExpiredAt = currentExpiredAt.plusMonths(monthsToAdd);
        }
        user.setStorageExpiredAt(newExpiredAt);

        userRepo.save(user);

        log.info("Nâng cấp hạn mức thành công cho User [{}]. Hạn mức: {} Bytes. Hết hạn: {}",
                user.getUsername(), newQuota, newExpiredAt);
    }

    // =========================
    // FAILED PAYMENT
    // =========================
    @Transactional
    public void processFailedPayment(String transactionId) {

        Transaction tx = transactionRepo.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (tx.getStatus() != TransactionStatus.PENDING) {
            return;
        }

        tx.setStatus(TransactionStatus.FAILED);
        tx.setUpdatedAt(LocalDateTime.now());

        transactionRepo.save(tx);
    }

    // =========================================================================
    // GIẢI QUYẾT LỖI: THÊM CÁC HÀM TIỆN ÍCH CHO USER THEO YÊU CẦU CONTROLLER
    // =========================================================================

    // 1. Hàm lấy danh sách gói cước hợp lệ (Chỉ hiển thị các gói lớn hơn hạn mức hiện tại)
    public List<StoragePlan> getAvailablePlans(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        long currentQuota = user.getStorageQuota() == null ? 524288000L : user.getStorageQuota();

        // Gọi repo lọc sạch các gói nhỏ hơn hoặc bằng
        return storagePlanRepo.findByQuotaSizeGreaterThan(currentQuota);
    }

    // 2. Hàm lấy chi tiết dung lượng và ngày hết hạn gói của bản thân User
    public Map<String, Object> getMyStorageDetails(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        long used = user.getStorageUsed() == null ? 0L : user.getStorageUsed();
        long max = user.getStorageQuota() == null ? 524288000L : user.getStorageQuota();
        long remaining = max - used;
        double percent = max > 0 ? ((double) used / max) * 100 : 0.0;

        Map<String, Object> details = new HashMap<>();
        details.put("usedStorage", used);
        details.put("maxStorage", max);
        details.put("remainingStorage", remaining);
        details.put("usagePercent", Math.round(percent * 100.0) / 100.0); // Làm tròn 2 chữ số thập phân
        details.put("expiredAt", user.getStorageExpiredAt()); // Trả về thời hạn (null nếu gói 500MB mặc định)

        if (user.getStorageExpiredAt() == null) {
            details.put("planStatus", "Gói Cơ Bản Mặc Định (Vĩnh viễn)");
        } else {
            details.put("planStatus", "Gói Nâng Cấp VIP (Có thời hạn)");
        }

        return details;
    }

    // =========================
    // GET STATUS
    // =========================
    public Transaction getTransactionStatus(String transactionId) {
        return transactionRepo.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));
    }

    // =========================
    // VALIDATE ROLE
    // =========================
    private void validateUserRole(User user) {
        boolean isUser = user.getRoles()
                .stream()
                .anyMatch(role -> "USER".equalsIgnoreCase(role.getName()));

        if (!isUser) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }
}