package com.pctb.webapp.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.CheckoutResponse;
import com.pctb.webapp.dto.response.UserStorageResponse;
import com.pctb.webapp.entity.StoragePlan;
import com.pctb.webapp.entity.Transaction;
import com.pctb.webapp.entity.TransactionStatus;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.TransactionRepo;
import com.pctb.webapp.service.PayOSService;
import com.pctb.webapp.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.WebhookData;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final PayOSService payOSService;
    private final TransactionRepo transactionRepo;

    // =========================
    // CHECKOUT
    // =========================
    @PostMapping("/checkout")
    public ApiResponse<CheckoutResponse> checkout(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam String planId,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        Transaction tx = paymentService.createPaymentIntent(
                jwt.getSubject(),
                planId,
                idempotencyKey
        );

        CreatePaymentLinkResponse res = payOSService.createPaymentLink(tx);

        return ApiResponse.<CheckoutResponse>builder()
                .result(new CheckoutResponse(
                        tx.getId(),
                        res.getCheckoutUrl(),
                        res.getAmount().longValue(),
                        res.getOrderCode()
                ))
                .build();
    }

    // =========================
    // WEBHOOK (FINAL FIX)
    // =========================
    @PostMapping("/webhook")
    public ApiResponse<String> handleWebhook(@RequestBody String body) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = (ObjectNode) mapper.readTree(body);

            WebhookData data = payOSService.verifyWebhook(node);

            Long orderCode = Long.valueOf(data.getOrderCode().toString());

            Transaction tx = transactionRepo.findByOrderCode(orderCode)
                    .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

            if (tx.getStatus() != TransactionStatus.PENDING) {
                return ApiResponse.<String>builder().result("IGNORED").build();
            }

            if ("00".equals(data.getCode())) {
                paymentService.processSuccessfulPayment(tx.getId());
            } else {
                paymentService.processFailedPayment(tx.getId());
            }

            return ApiResponse.<String>builder().result("OK").build();

        } catch (Exception e) {
            log.error("PAYOS WEBHOOK ERROR", e);
            return ApiResponse.<String>builder().result("ERROR").message(e.getMessage()).build();
        }
    }

    // =========================
    // STATUS
    // =========================
    @GetMapping("/status/{id}")
    public ApiResponse<TransactionStatus> status(@PathVariable String id) {
        Transaction tx = paymentService.getTransactionStatus(id);
        return ApiResponse.<TransactionStatus>builder().result(tx.getStatus()).build();
    }

    // =========================================================================
    // TIỆN ÍCH USER: LẤY DANH SÁCH GÓI VIP ĐƯỢC PHÉP NÂNG CẤP (ẨN CÁC GÓI THẤP HƠN)
    // =========================================================================
    @GetMapping("/available-plans")
    public ApiResponse<List<StoragePlan>> getAvailablePlans(@AuthenticationPrincipal Jwt jwt) {
        List<StoragePlan> plans = paymentService.getAvailablePlansForUser(jwt.getSubject());
        return ApiResponse.<List<StoragePlan>>builder().result(plans).build();
    }

    // =========================================================================
    // TIỆN ÍCH USER: XEM THÔNG TIN DUNG LƯỢNG THỰC TẾ ĐÃ SỬ DỤNG VÀ THỜI HẠN GÓI
    // =========================================================================
    @GetMapping("/my-storage")
    public ApiResponse<UserStorageResponse> getMyStorageDetails(@AuthenticationPrincipal Jwt jwt) {
        UserStorageResponse storageDetails = paymentService.getMyStorageDetails(jwt.getSubject());
        return ApiResponse.<UserStorageResponse>builder().result(storageDetails).build();
    }
}