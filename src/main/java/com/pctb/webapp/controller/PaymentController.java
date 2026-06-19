package com.pctb.webapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pctb.webapp.dto.request.PayOSWebhookRequest;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.CheckoutResponse;
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
                        res.getAmount().intValue(),
                        res.getOrderCode()
                ))
                .build();
    }

    // =========================
    // WEBHOOK (FIXED)
    // =========================
    @PostMapping("/webhook")
    public ApiResponse<String> handleWebhook(@RequestBody PayOSWebhookRequest body) {

        try {
            log.info("PAYOS WEBHOOK: {}", body);

            // convert DTO -> ObjectNode (để match PayOS SDK cũ)
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode node = mapper.valueToTree(body);

            WebhookData data = payOSService.verifyWebhook(node);

            Long orderCode = Long.valueOf(data.getOrderCode().toString());

            Transaction tx = transactionRepo.findByOrderCode(orderCode)
                    .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));

            log.info("Webhook txId={}, status={}, code={}",
                    tx.getId(), tx.getStatus(), data.getCode());

            // =========================
            // IDEMPOTENCY
            // =========================
            if (tx.getStatus() != TransactionStatus.PENDING) {
                return ApiResponse.<String>builder()
                        .result("IGNORED")
                        .build();
            }

            // =========================
            // PROCESS RESULT
            // =========================
            if ("00".equals(data.getCode())) {
                paymentService.processSuccessfulPayment(tx.getId());
            } else {
                paymentService.processFailedPayment(tx.getId());
            }

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

    // =========================
    // STATUS
    // =========================
    @GetMapping("/status/{id}")
    public ApiResponse<TransactionStatus> status(@PathVariable String id) {

        return ApiResponse.<TransactionStatus>builder()
                .result(paymentService.getTransactionStatus(id).getStatus())
                .build();
    }
}