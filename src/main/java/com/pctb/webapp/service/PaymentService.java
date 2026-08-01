package com.pctb.webapp.service;

import com.pctb.webapp.entity.StoragePlan;
import com.pctb.webapp.entity.Transaction;
import com.pctb.webapp.entity.TransactionStatus;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.dto.response.PaymentResultResponse;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.StoragePlanRepo;
import com.pctb.webapp.repository.TransactionRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.payment.checkout-expiry-seconds:30}")
    private long checkoutExpirySeconds = 30;

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

        LocalDateTime now = LocalDateTime.now();
        Transaction transaction = Transaction.builder()
                .id(UUID.randomUUID().toString())
                .orderCode(System.currentTimeMillis())
                .user(user)
                .plan(plan)
                .amount(plan.getPrice())
                .quotaAdded(plan.getQuotaSize())
                .status(TransactionStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .createdAt(now)
                .expiredAt(now.plusSeconds(checkoutExpirySeconds))
                .build();

        return transactionRepo.save(transaction);
    }

    @Transactional
    public boolean processSuccessfulPayment(String transactionId) {
        Transaction tx = transactionRepo.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        if (tx.getStatus() == TransactionStatus.SUCCESS) {
            return false;
        }
        if (tx.getStatus() != TransactionStatus.PENDING) {
            return false;
        }

        grantPlanAndMarkSuccess(tx);
        return true;
    }

    private void grantPlanAndMarkSuccess(Transaction tx) {
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

    @Transactional
    public PaymentResultResponse processPaymentCallback(Long orderCode, String gatewayStatus, String gatewayCode) {
        if (orderCode == null) {
            throw new AppException(ErrorCode.REQUEST_PARAMETER_INVALID);
        }

        Transaction tx = transactionRepo.findByOrderCodeForUpdate(orderCode)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

        CallbackStatus callbackStatus = resolveCallbackStatus(gatewayStatus, gatewayCode);
        if (callbackStatus == CallbackStatus.UNKNOWN && isExpired(tx)) {
            callbackStatus = CallbackStatus.EXPIRED;
        }
        boolean planGranted = false;

        if (callbackStatus == CallbackStatus.SUCCESS && tx.getStatus() == TransactionStatus.PENDING) {
            grantPlanAndMarkSuccess(tx);
            planGranted = true;
        } else if (callbackStatus == CallbackStatus.CANCELLED && tx.getStatus() == TransactionStatus.PENDING) {
            updateTransactionStatus(tx, TransactionStatus.CANCELLED);
        } else if (callbackStatus == CallbackStatus.FAILED && tx.getStatus() == TransactionStatus.PENDING) {
            updateTransactionStatus(tx, TransactionStatus.FAILED);
        } else if (callbackStatus == CallbackStatus.EXPIRED && tx.getStatus() == TransactionStatus.PENDING) {
            updateTransactionStatus(tx, TransactionStatus.TIMEOUT);
        }

        return buildPaymentResultResponse(tx, callbackStatus, planGranted);
    }

    private void updateTransactionStatus(Transaction tx, TransactionStatus status) {
        tx.setStatus(status);
        tx.setUpdatedAt(LocalDateTime.now());
        transactionRepo.save(tx);
    }

    private CallbackStatus resolveCallbackStatus(String gatewayStatus, String gatewayCode) {
        String normalizedStatus = normalizeStatusToken(gatewayStatus);
        if (!normalizedStatus.isBlank()) {
            return switch (normalizedStatus) {
                case "SUCCESS", "PAID" -> CallbackStatus.SUCCESS;
                case "CANCELLED", "CANCELED" -> CallbackStatus.CANCELLED;
                case "FAILED" -> CallbackStatus.FAILED;
                case "EXPIRED", "TIMEOUT" -> CallbackStatus.EXPIRED;
                case "PENDING" -> CallbackStatus.PENDING;
                default -> CallbackStatus.UNKNOWN;
            };
        }

        String normalizedCode = normalizeStatusToken(gatewayCode);
        if ("00".equals(normalizedCode)) {
            return CallbackStatus.SUCCESS;
        }
        if (!normalizedCode.isBlank()) {
            return CallbackStatus.FAILED;
        }
        return CallbackStatus.UNKNOWN;
    }

    private String normalizeStatusToken(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private PaymentResultResponse buildPaymentResultResponse(
            Transaction tx,
            CallbackStatus callbackStatus,
            boolean planGranted
    ) {
        TransactionStatus status = tx.getStatus() == null ? TransactionStatus.PENDING : tx.getStatus();
        String responseStatus = responseStatus(status, callbackStatus);

        return PaymentResultResponse.builder()
                .transactionId(tx.getId())
                .orderCode(tx.getOrderCode())
                .status(responseStatus)
                .displayStatus(displayStatus(responseStatus))
                .title(resultTitle(responseStatus))
                .message(resultMessage(responseStatus))
                .planGranted(planGranted)
                .build();
    }

    private String responseStatus(TransactionStatus status, CallbackStatus callbackStatus) {
        if (status == TransactionStatus.TIMEOUT) {
            return "EXPIRED";
        }
        if (callbackStatus == CallbackStatus.UNKNOWN && status == TransactionStatus.PENDING) {
            return "PENDING";
        }
        return status.name();
    }

    private String displayStatus(String status) {
        return switch (normalizeStatusToken(status)) {
            case "SUCCESS", "PAID" -> "Thành công";
            case "CANCELLED", "CANCELED" -> "Đã hủy";
            case "FAILED" -> "Thất bại";
            case "EXPIRED", "TIMEOUT" -> "Hết hạn";
            default -> "Chờ xử lý";
        };
    }

    private String resultTitle(String status) {
        return switch (normalizeStatusToken(status)) {
            case "SUCCESS", "PAID" -> "Đã thanh toán";
            case "CANCELLED", "CANCELED" -> "Đã hủy thanh toán";
            case "FAILED" -> "Thanh toán thất bại";
            case "EXPIRED", "TIMEOUT" -> "Thanh toán đã hết hạn";
            default -> "Đang chờ thanh toán";
        };
    }

    private String resultMessage(String status) {
        return switch (normalizeStatusToken(status)) {
            case "SUCCESS", "PAID" -> "Giao dịch đã được xác nhận thành công. Gói lưu trữ của bạn đã được cập nhật.";
            case "CANCELLED", "CANCELED" -> "Giao dịch đã bị hủy. Không có khoản phí nào được tính vào tài khoản của bạn.";
            case "FAILED" -> "Giao dịch không thành công. Gói lưu trữ chưa được cập nhật.";
            case "EXPIRED", "TIMEOUT" -> "Phiên thanh toán đã hết hạn. Gói lưu trữ chưa được cập nhật.";
            default -> "Giao dịch đang chờ xác nhận. Gói lưu trữ sẽ chỉ được cập nhật sau khi thanh toán thành công.";
        };
    }

    @Transactional
    public Transaction getTransactionStatus(String transactionId) {
        Transaction tx = transactionRepo.findById(transactionId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));
        if (isExpired(tx)) {
            updateTransactionStatus(tx, TransactionStatus.TIMEOUT);
        }
        return tx;
    }

    @Transactional
    public int markExpiredPendingTransactions() {
        return transactionRepo.markExpiredPendingTransactions(LocalDateTime.now());
    }

    private boolean isExpired(Transaction tx) {
        return tx.getStatus() == TransactionStatus.PENDING
                && tx.getExpiredAt() != null
                && !tx.getExpiredAt().isAfter(LocalDateTime.now());
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

    private enum CallbackStatus {
        SUCCESS,
        CANCELLED,
        FAILED,
        EXPIRED,
        PENDING,
        UNKNOWN
    }
}
