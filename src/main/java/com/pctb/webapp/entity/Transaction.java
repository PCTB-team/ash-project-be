package com.pctb.webapp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment_transaction", indexes = {
        @Index(name = "idx_tx_idempotency", columnList = "idempotencyKey", unique = true),
        @Index(name = "idx_order_code", columnList = "orderCode")
})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Transaction {

    @Id
    private String id;

    @Column(unique = true, nullable = false)
    private Long orderCode;

    @ManyToOne(fetch = FetchType.LAZY)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private StoragePlan plan;

    // ✅ FIX: tiền dùng Long (VND)
    private Long amount; // VND (không dùng Double)

    private Long quotaAdded;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    @Column(unique = true)
    private String idempotencyKey;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}