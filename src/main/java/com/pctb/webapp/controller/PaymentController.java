package com.pctb.webapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.CheckoutResponse;
import com.pctb.webapp.entity.StoragePlan;
import com.pctb.webapp.entity.Transaction;
import com.pctb.webapp.entity.TransactionStatus;
import com.pctb.webapp.repository.TransactionRepo;
import com.pctb.webapp.service.PayOSService;
import com.pctb.webapp.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken; // Import đồng bộ
import org.springframework.web.bind.annotation.*;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.webhooks.WebhookData;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final PayOSService payOSService;
    private final TransactionRepo transactionRepo;
    private final ObjectMapper objectMapper;

    // =========================================================================
    // CHECKOUT (ĐÃ ĐỒNG BỘ TOKEN CHỨNG THỰC)
    // =========================================================================
    @PostMapping("/checkout")
    public ApiResponse<CheckoutResponse> checkout(
            JwtAuthenticationToken token, // Đổi từ Jwt sang JwtAuthenticationToken để đồng bộ với AdminController
            @RequestParam String planId,
            @RequestHeader("Idempotency-Key") String idempotencyKey
    ) {
        // token.getName() tương ứng với Subject (userId) định danh người dùng trong hệ thống của bạn
        String userId = token.getName();

        Transaction tx = paymentService.createPaymentIntent(
                userId,
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

    // =========================================================================
    // WEBHOOK (FINAL FIXED)
    // =========================================================================
    @PostMapping("/webhook")
    public ApiResponse<String> handleWebhook(
            @RequestBody String body,
            @RequestHeader(value = "x-payos-signature", required = false) String signature
    ) {
        log.info("PAYOS WEBHOOK RAW: {}", body);
        log.info("PAYOS SIGNATURE: {}", signature);

        try {
            if (body == null || body.isBlank()) {
                return ApiResponse.<String>builder()
                        .result("ERROR")
                        .message("Empty payload")
                        .build();
            }

            JsonNode root = objectMapper.readTree(body);
            WebhookData data = payOSService.verifyWebhook(root, signature);
            Long orderCode = data.getOrderCode();

            transactionRepo.findByOrderCode(orderCode).ifPresentOrElse(tx -> {
                if ("00".equals(data.getCode())) {
                    paymentService.processSuccessfulPayment(tx.getId());
                    log.info("PAYMENT SUCCESS orderCode={}", orderCode);
                } else {
                    paymentService.processFailedPayment(tx.getId());
                    log.info("PAYMENT FAILED orderCode={}", orderCode);
                }
            }, () -> log.error("TRANSACTION NOT FOUND orderCode={}", orderCode));

            return ApiResponse.<String>builder()
                    .result("OK")
                    .build();

        } catch (Exception e) {
            log.error("PAYOS WEBHOOK ERROR", e);
            return ApiResponse.<String>builder()
                    .result("ERROR")
                    .message(e.getMessage())
                    .build();
        }
    }

    // =========================================================================
    // STATUS
    // =========================================================================
    @GetMapping("/status/{id}")
    public ApiResponse<TransactionStatus> status(@PathVariable String id) {
        Transaction tx = paymentService.getTransactionStatus(id);

        return ApiResponse.<TransactionStatus>builder()
                .result(tx.getStatus())
                .build();
    }

    // =========================================================================
    // TIỆN ÍCH USER: LẤY DANH SÁCH CÁC GÓI HỢP LỆ CÓ THỂ NÂNG CẤP
    // =========================================================================
    @GetMapping("/available-plans")
    public ApiResponse<List<StoragePlan>> getAvailablePlans(JwtAuthenticationToken token) {
        String userId = token.getName(); // Lấy userId từ Token đồng bộ
        List<StoragePlan> plans = paymentService.getAvailablePlans(userId);

        return ApiResponse.<List<StoragePlan>>builder()
                .result(plans)
                .build();
    }

    // =========================================================================
    // TIỆN ÍCH USER: XEM CHI TIẾT DUNG LƯỢNG VÀ THỜI HẠN GÓI CỦA BẢN THÂN
    // =========================================================================
    @GetMapping("/my-storage")
    public ApiResponse<Map<String, Object>> getMyStorageDetails(JwtAuthenticationToken token) {
        String userId = token.getName();
        Map<String, Object> storageDetails = paymentService.getMyStorageDetails(userId);

        return ApiResponse.<Map<String, Object>>builder()
                .result(storageDetails)
                .build();
    }
}