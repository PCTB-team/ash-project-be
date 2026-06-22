package com.pctb.webapp.service;

import com.pctb.webapp.dto.response.UserStorageResponse;
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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepo transactionRepo;
    private final UserRepo userRepo;
    private final StoragePlanRepo storagePlanRepo;

    // =========================================================================
    // CREATE INTENT (KHỐNG CHẾ BẬC THANG CHẶN ĐÈ)
    // =========================================================================
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

        // CHẶN BẬC THANG: Không cho phép mua gói có dung lượng nhỏ hơn hoặc bằng dung lượng hiện tại
        long currentQuota = user.getStorageQuota() == null ? 524288000L : user.getStorageQuota();
        if (plan.getQuotaSize() <= currentQuota) {
            throw new AppException(ErrorCode.PLAN_LEVEL_LOW);
        }

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .orderCode(System.currentTimeMillis())
                .user(user)
                .plan(plan)
                .amount(plan.getPrice())
                .quotaAdded(plan.getQuotaSize()) // Lưu hạn mức mới để xử lý kích hoạt sau này
                .status(TransactionStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .build();

        return transactionRepo.save(transaction);
    }

    // =========================================================================
    // SUCCESS PAYMENT (THAY THẾ DUNG LƯỢNG & CỘNG DỒN CHU KỲ THỜI GIAN)
    // =========================================================================
    @Transactional
    public void processSuccessfulPayment(String transactionId) {

        Transaction tx = transactionRepo.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (tx.getStatus() == TransactionStatus.SUCCESS) {
            return; // Đã xử lý rồi thì bỏ qua chống cộng lặp trùng
        }

        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setUpdatedAt(LocalDateTime.now());

        User user = tx.getUser();
        StoragePlan plan = tx.getPlan();

        // 1. Cập nhật đè dung lượng gói mới (Khống chế trần Cloudinary)
        user.setStorageQuota(tx.getQuotaAdded());

        // 2. Tính toán ngày hết hạn (Chu kỳ Tháng/Năm)
        int monthsToAdd = plan.getDurationMonths() != null ? plan.getDurationMonths() : 1;
        LocalDateTime currentExpiredAt = user.getStorageExpiredAt();
        LocalDateTime newExpiredAt;

        // Nếu chưa từng mua VIP hoặc gói cũ đã hết hạn từ trước -> Tính chu kỳ mới từ hôm nay
        if (currentExpiredAt == null || currentExpiredAt.isBefore(LocalDateTime.now())) {
            newExpiredAt = LocalDateTime.now().plusMonths(monthsToAdd);
        } else {
            // Nếu gói cũ vẫn đang còn hạn sử dụng -> Cộng nối tiếp thời gian sử dụng vào ngày cũ (Gia hạn)
            newExpiredAt = currentExpiredAt.plusMonths(monthsToAdd);
        }

        user.setStorageExpiredAt(newExpiredAt);

        userRepo.save(user);
        transactionRepo.save(tx);

        log.info("Kích hoạt VIP thành công cho User [{}]. Dung lượng mới: {} Bytes, Hết hạn: {}",
                user.getUsername(), tx.getQuotaAdded(), newExpiredAt);
    }

    @Transactional
    public void processFailedPayment(String transactionId) {
        Transaction tx = transactionRepo.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (tx.getStatus() != TransactionStatus.PENDING) return;

        tx.setStatus(TransactionStatus.FAILED);
        tx.setUpdatedAt(LocalDateTime.now());
        transactionRepo.save(tx);
    }

    public Transaction getTransactionStatus(String transactionId) {
        return transactionRepo.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));
    }

    // =========================================================================
    // TIỆN ÍCH USER: TRẢ VỀ CÁC CƠ CHẾ HIỂN THỊ TRÊN GIAO DIỆN FRONT-END
    // =========================================================================

    // Lấy các gói cước VIP cao hơn cấp hiện tại để hiển thị lên bảng nâng cấp cho User lựa chọn
    public List<StoragePlan> getAvailablePlansForUser(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        long currentQuota = user.getStorageQuota() == null ? 524288000L : user.getStorageQuota();
        return storagePlanRepo.findAvailableUpgrades(currentQuota);
    }

    // Xem chi tiết bộ nhớ dung lượng đã xài, dung lượng trống, tỷ lệ phần trăm trực quan kèm ngày hết hạn
    public UserStorageResponse getMyStorageDetails(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        long used = user.getStorageUsed() == null ? 0L : user.getStorageUsed();
        long max = user.getStorageQuota() == null ? 524288000L : user.getStorageQuota();
        long remaining = max - used;
        if (remaining < 0) remaining = 0L;

        double percent = max > 0 ? ((double) used / max) * 100 : 0.0;
        percent = Math.round(percent * 100.0) / 100.0; // Làm tròn về 2 chữ số thập phân

        return UserStorageResponse.builder()
                .usedStorage(used)
                .maxStorage(max)
                .remainingStorage(remaining)
                .usagePercent(percent)
                .build();
    }

    private void validateUserRole(User user) {
        boolean isUser = user.getRoles().stream()
                .anyMatch(role -> "USER".equalsIgnoreCase(role.getName()));
        if (!isUser) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }
}