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
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepo transactionRepo;
    private final UserRepo userRepo;
    private final StoragePlanRepo storagePlanRepo;

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
                .quotaAdded(plan.getQuotaSize())
                .status(TransactionStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .build();

        return transactionRepo.save(transaction);
    }

    @Transactional
    public void processSuccessfulPayment(String transactionId) {
        Transaction tx = transactionRepo.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (tx.getStatus() == TransactionStatus.SUCCESS) {
            return;
        }

        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setUpdatedAt(LocalDateTime.now());

        User user = tx.getUser();
        StoragePlan plan = tx.getPlan();

        // 🟢 ĐỒNG BỘ GỐC: Ghi trực tiếp vào trường dữ liệu của User để nuôi cho UserService của nhóm đọc lên
        user.setStorageQuota(tx.getQuotaAdded());

        int monthsToAdd = plan.getDurationMonths() != null ? plan.getDurationMonths() : 1;
        LocalDateTime currentExpiredAt = user.getStorageExpiredAt();
        LocalDateTime newExpiredAt;

        if (currentExpiredAt == null || currentExpiredAt.isBefore(LocalDateTime.now())) {
            newExpiredAt = LocalDateTime.now().plusMonths(monthsToAdd);
        } else {
            newExpiredAt = currentExpiredAt.plusMonths(monthsToAdd);
        }
        user.setStorageExpiredAt(newExpiredAt);

        userRepo.save(user);
        transactionRepo.save(tx);
        log.info("VIP synced successfully for User [{}]. Quota updated directly in User Entity.", user.getUsername());
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

    private void validateUserRole(User user) {
        boolean isUser = user.getRoles().stream()
                .anyMatch(role -> "USER".equalsIgnoreCase(role.getName()));
        if (!isUser) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
    }

    // 🟢 BỔ SUNG LẠI: Lấy danh sách toàn bộ gói cước VIP có trong hệ thống để hiển thị lên bảng giá
    public List<StoragePlan> getAllAvailablePlans() {
        return storagePlanRepo.findAll();
    }
}