package com.pctb.webapp.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MonthlyStatItem {
    String name;  // Tên tháng (Ví dụ: "Tháng 1", "Tháng 2"...)
    double value; // Số lượng user mới hoặc số doanh thu tương ứng của tháng đó
}