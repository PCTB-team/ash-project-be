package com.pctb.webapp.service;

import com.fasterxml.jackson.databind.node.ObjectNode;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayOSService {

    private final PayOS payOS;

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
    // VERIFY WEBHOOK (SAFE)
    // =========================
    public WebhookData verifyWebhook(ObjectNode body) {

        try {
            if (body == null || body.isEmpty()) {
                throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
            }

            return payOS.webhooks().verify(body);

        } catch (Exception e) {
            log.error("PayOS webhook signature invalid. body={}", body, e);
            throw new AppException(ErrorCode.PAYMENT_GATEWAY_ERROR);
        }
    }
}