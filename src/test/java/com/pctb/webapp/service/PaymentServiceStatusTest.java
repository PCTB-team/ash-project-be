package com.pctb.webapp.service;

import com.pctb.webapp.dto.response.PaymentResultResponse;
import com.pctb.webapp.entity.StoragePlan;
import com.pctb.webapp.entity.Transaction;
import com.pctb.webapp.entity.TransactionStatus;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.repository.StoragePlanRepo;
import com.pctb.webapp.repository.TransactionRepo;
import com.pctb.webapp.repository.UserRepo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceStatusTest {
    private final TransactionRepo transactionRepo = mock(TransactionRepo.class);
    private final UserRepo userRepo = mock(UserRepo.class);
    private final StoragePlanRepo storagePlanRepo = mock(StoragePlanRepo.class);
    private final PaymentService paymentService = new PaymentService(transactionRepo, userRepo, storagePlanRepo);

    @Test
    void cancelledCallbackMarksTransactionCancelledAndDoesNotGrantPlan() {
        Transaction tx = pendingTransaction();
        when(transactionRepo.findByOrderCodeForUpdate(123L)).thenReturn(Optional.of(tx));

        PaymentResultResponse response = paymentService.processPaymentCallback(123L, "CANCELLED", null);

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.CANCELLED);
        assertThat(response.getTitle()).isEqualTo("Đã hủy thanh toán");
        assertThat(response.getMessage()).isEqualTo("Giao dịch đã bị hủy. Không có khoản phí nào được tính vào tài khoản của bạn.");
        assertThat(response.isPlanGranted()).isFalse();
        verify(userRepo, never()).save(tx.getUser());
    }

    @Test
    void canceledAliasIsTreatedAsCancelled() {
        Transaction tx = pendingTransaction();
        when(transactionRepo.findByOrderCodeForUpdate(123L)).thenReturn(Optional.of(tx));

        PaymentResultResponse response = paymentService.processPaymentCallback(123L, "CANCELED", null);

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.CANCELLED);
        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        assertThat(response.getTitle()).isEqualTo("Đã hủy thanh toán");
        assertThat(response.isPlanGranted()).isFalse();
        verify(userRepo, never()).save(tx.getUser());
    }

    @Test
    void repeatedCancelledCallbackDoesNotGrantPlan() {
        Transaction tx = pendingTransaction();
        when(transactionRepo.findByOrderCodeForUpdate(123L)).thenReturn(Optional.of(tx));

        PaymentResultResponse first = paymentService.processPaymentCallback(123L, "CANCELLED", null);
        PaymentResultResponse second = paymentService.processPaymentCallback(123L, "CANCELLED", null);

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.CANCELLED);
        assertThat(first.isPlanGranted()).isFalse();
        assertThat(second.isPlanGranted()).isFalse();
        assertThat(second.getTitle()).isEqualTo("Đã hủy thanh toán");
        verify(userRepo, never()).save(tx.getUser());
    }

    @Test
    void successCallbackGrantsPlanOnlyOnce() {
        Transaction tx = pendingTransaction();
        when(transactionRepo.findByOrderCodeForUpdate(123L)).thenReturn(Optional.of(tx));

        PaymentResultResponse first = paymentService.processPaymentCallback(123L, "SUCCESS", null);
        PaymentResultResponse second = paymentService.processPaymentCallback(123L, "SUCCESS", null);

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(first.isPlanGranted()).isTrue();
        assertThat(second.isPlanGranted()).isFalse();
        assertThat(first.getTitle()).isEqualTo("Đã thanh toán");
        verify(userRepo).save(tx.getUser());
    }

    @Test
    void codeZeroZeroIsTreatedAsSuccessWhenStatusIsMissing() {
        Transaction tx = pendingTransaction();
        when(transactionRepo.findByOrderCodeForUpdate(123L)).thenReturn(Optional.of(tx));

        PaymentResultResponse response = paymentService.processPaymentCallback(123L, null, "00");

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(response.getTitle()).isEqualTo("Đã thanh toán");
        assertThat(response.isPlanGranted()).isTrue();
        verify(userRepo).save(tx.getUser());
    }

    @Test
    void paidCallbackIsTreatedAsSuccess() {
        Transaction tx = pendingTransaction();
        when(transactionRepo.findByOrderCodeForUpdate(123L)).thenReturn(Optional.of(tx));

        PaymentResultResponse response = paymentService.processPaymentCallback(123L, "PAID", null);

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.SUCCESS);
        assertThat(response.getDisplayStatus()).isEqualTo("Thành công");
        assertThat(response.isPlanGranted()).isTrue();
    }

    @Test
    void failedCallbackMarksTransactionFailedAndDoesNotGrantPlan() {
        Transaction tx = pendingTransaction();
        when(transactionRepo.findByOrderCodeForUpdate(123L)).thenReturn(Optional.of(tx));

        PaymentResultResponse response = paymentService.processPaymentCallback(123L, "FAILED", null);

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(response.getTitle()).isEqualTo("Thanh toán thất bại");
        assertThat(response.isPlanGranted()).isFalse();
        verify(userRepo, never()).save(tx.getUser());
    }

    @Test
    void pendingCallbackKeepsTransactionPendingAndDoesNotGrantPlan() {
        Transaction tx = pendingTransaction();
        when(transactionRepo.findByOrderCodeForUpdate(123L)).thenReturn(Optional.of(tx));

        PaymentResultResponse response = paymentService.processPaymentCallback(123L, "PENDING", null);

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(response.getTitle()).isEqualTo("Đang chờ thanh toán");
        assertThat(response.isPlanGranted()).isFalse();
        verify(userRepo, never()).save(tx.getUser());
    }

    @Test
    void expiredCallbackStoresTimeoutAndReturnsExpiredDisplayStatus() {
        Transaction tx = pendingTransaction();
        when(transactionRepo.findByOrderCodeForUpdate(123L)).thenReturn(Optional.of(tx));

        PaymentResultResponse response = paymentService.processPaymentCallback(123L, "EXPIRED", null);

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.TIMEOUT);
        assertThat(response.getStatus()).isEqualTo("EXPIRED");
        assertThat(response.getDisplayStatus()).isEqualTo("Hết hạn");
        assertThat(response.isPlanGranted()).isFalse();
        verify(userRepo, never()).save(tx.getUser());
    }

    @Test
    void missingStatusDoesNotDefaultToSuccess() {
        Transaction tx = pendingTransaction();
        when(transactionRepo.findByOrderCodeForUpdate(123L)).thenReturn(Optional.of(tx));

        PaymentResultResponse response = paymentService.processPaymentCallback(123L, null, null);

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(response.getTitle()).isEqualTo("Đang chờ thanh toán");
        assertThat(response.isPlanGranted()).isFalse();
        verify(userRepo, never()).save(tx.getUser());
    }

    @Test
    void missingStatusMarksExpiredPendingTransactionAsTimeout() {
        Transaction tx = pendingTransaction();
        tx.setExpiredAt(LocalDateTime.now().minusMinutes(1));
        when(transactionRepo.findByOrderCodeForUpdate(123L)).thenReturn(Optional.of(tx));

        PaymentResultResponse response = paymentService.processPaymentCallback(123L, null, null);

        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.TIMEOUT);
        assertThat(response.getStatus()).isEqualTo("EXPIRED");
        assertThat(response.isPlanGranted()).isFalse();
        verify(userRepo, never()).save(tx.getUser());
    }

    private Transaction pendingTransaction() {
        User user = User.builder()
                .id("user-1")
                .username("student")
                .storageQuota(524288000L)
                .build();
        StoragePlan plan = StoragePlan.builder()
                .id("plan-1")
                .planName("PRO")
                .quotaSize(1073741824L)
                .price(100000L)
                .durationMonths(1)
                .build();
        return Transaction.builder()
                .id("tx-1")
                .orderCode(123L)
                .user(user)
                .plan(plan)
                .amount(100000L)
                .quotaAdded(plan.getQuotaSize())
                .status(TransactionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusSeconds(30))
                .build();
    }
}
