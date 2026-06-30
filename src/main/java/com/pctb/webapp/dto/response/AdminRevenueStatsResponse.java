package com.pctb.webapp.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminRevenueStatsResponse {
    String granularity;
    LocalDateTime from;
    LocalDateTime to;
    long totalRevenue;
    long transactionCount;
    double averageOrderValue;
    List<AdminRevenueStatItem> series;
}
