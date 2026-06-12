package com.pctb.webapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private long totalUsers;
    private Map<String, Long> userGrowth; // Ví dụ: {"last7Days": 10, "last2Weeks": 20, ...}
}