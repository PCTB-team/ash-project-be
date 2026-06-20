package com.pctb.webapp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pctb.webapp.entity.Transaction;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.webhooks.WebhookData;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayOSService {

    private final PayOS payOS;
    private final ObjectMapper objectMapper;

    // ⚠️ lấy từ env production
    private final String CHECKSUM_KEY = "YOUR_PAYOS_CHECKSUM_KEY";

    // =========================
    // CREATE PAYMENT LINK
    // =========================
    public CreatePaymentLinkResponse createPaymentLink(Transaction tx) {

        try {
            if (tx == null) {
                throw new AppException(ErrorCode.TRANSACTION_NOT_FOUND);
            }

            long amount = tx.getAmount() == null ? 0L : tx.getAmount();

            if (amount <= 0) {
                throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
            }

            PaymentLinkItem item = PaymentLinkItem.builder()
                    .name("Storage Upgrade Plan")
                    .quantity(1)
                    .price(amount)
                    .build();

            CreatePaymentLinkRequest request = CreatePaymentLinkRequest.builder()
                    .orderCode(tx.getOrderCode())
                    .amount(amount)
                    .description("Upgrade storage plan")
                    .returnUrl("https://ash-project-fe.vercel.app/payment/success")
                    .cancelUrl("https://ash-project-fe.vercel.app/payment/cancel")
                    .items(List.of(item))
                    .build();

            return payOS.paymentRequests().create(request);

        } catch (Exception e) {
            log.error("PayOS createPaymentLink failed - txId={}",
                    tx != null ? tx.getId() : null, e);

            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
    }

    // =========================
    // WEBHOOK VERIFY (PRODUCTION FIX)
    // =========================
    public WebhookData verifyWebhook(JsonNode body, String signature) {

        try {
            String rawData = body.toString();

            // 1. verify signature
            if (!verifySignature(rawData, signature)) {
                throw new RuntimeException("Invalid PayOS signature");
            }

            // 2. map to WebhookData
            return objectMapper.treeToValue(body, WebhookData.class);

        } catch (Exception e) {
            log.error("Webhook verification failed", e);
            throw new RuntimeException("Webhook verification failed", e);
        }
    }

    // =========================
    // SIGNATURE VERIFY (HMAC SHA256)
    // =========================
    private boolean verifySignature(String data, String signature) {

        try {
            if (signature == null || signature.isBlank()) return false;

            Mac mac = Mac.getInstance("HmacSHA256");

            SecretKeySpec secretKey = new SecretKeySpec(
                    CHECKSUM_KEY.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );

            mac.init(secretKey);

            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            String computed = Base64.getEncoder().encodeToString(hash);

            return computed.equals(signature);

        } catch (Exception e) {
            log.error("Signature verify error", e);
            return false;
        }
    }
}