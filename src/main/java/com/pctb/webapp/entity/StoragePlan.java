package com.pctb.webapp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "storage_plan")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StoragePlan {

    @Id
    String id;

    @Column(nullable = false)
    String planName;

    @Column(nullable = false)
    Long quotaSize; // Tính bằng đơn vị Byte

    @Column(nullable = false)
    Long price; // VND

    // === BỔ SUNG QUẢN LÝ CHU KỲ THEO THÁNG ===
    @Column(name = "duration_months", nullable = false)
    Integer durationMonths; // 1: 1 Tháng, 12: 1 Năm
}