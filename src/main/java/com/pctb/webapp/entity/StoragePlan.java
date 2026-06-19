package com.pctb.webapp.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * THỰC THỂ LƯU TRỮ THÔNG TIN CÁC GÓI NÂNG CẤP DUNG LƯỢNG VIP
 * @author SWP391_PCTB_Team
 */
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
    String id; // Mã định danh duy nhất của gói (VD: PLAN_10GB, PLAN_50GB)

    @Column(nullable = false)
    String planName; // Tên hiển thị trực quan của gói dịch vụ nâng cấp

    @Column(nullable = false)
    Long quotaSize; // Dung lượng được cộng thêm vào tài khoản, tính bằng đơn vị Byte

    @Column(nullable = false)
    Long price; // VND (long) // Giá tiền tương ứng của gói dịch vụ
}