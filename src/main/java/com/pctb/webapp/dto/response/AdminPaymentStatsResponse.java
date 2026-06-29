package com.pctb.webapp.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminPaymentStatsResponse {
    long revenueThisMonth;
    long successfulTransactionCount;
    long failedOrCancelledTransactionCount;
    double averageRevenuePerUser;
}
