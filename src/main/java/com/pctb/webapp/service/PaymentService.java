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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final TransactionRepo transactionRepo;
    private final UserRepo userRepo;
    private final StoragePlanRepo storagePlanRepo;

    // =========================
    // CREATE PAYMENT INTENT
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

        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .orderCode(System.currentTimeMillis())
                .user(user)
                .plan(plan)
                .amount(plan.getPrice().longValue())
                .quotaAdded(plan.getQuotaSize())
                .status(TransactionStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .createdAt(LocalDateTime.now())
                .build();

        return transactionRepo.save(transaction);
    }

    // =========================
    // SUCCESS PAYMENT
    // =========================
    @Transactional
    public void processSuccessfulPayment(String transactionId) {

        Transaction tx = transactionRepo.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (tx.getStatus() != TransactionStatus.PENDING) {
            return;
        }

        tx.setStatus(TransactionStatus.SUCCESS);
        tx.setUpdatedAt(LocalDateTime.now());

        User user = tx.getUser();

        long currentQuota = user.getStorageQuota() == null ? 0L : user.getStorageQuota();
        user.setStorageQuota(currentQuota + tx.getQuotaAdded());

        userRepo.save(user);
        transactionRepo.save(tx);
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

    // =========================
    // UPDATE STORAGE (OPTIONAL UTILITY)
    // =========================
    private void updateUserStorage(User user, Long additionalQuota) {

        long current = user.getStorageQuota() == null ? 0L : user.getStorageQuota();

        user.setStorageQuota(current + (additionalQuota == null ? 0L : additionalQuota));

        userRepo.save(user);
    }
}