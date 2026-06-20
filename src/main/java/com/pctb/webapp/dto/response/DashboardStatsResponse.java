package com.pctb.webapp.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    // --- 1. Thống kê User ---
    private long totalUsers;
    private Map<String, Long> userGrowth; // Ví dụ: {"last7Days": 10, "last2Weeks": 20, ...}

    // --- 2. Admin (Doanh thu & Giao dịch) ---
    double totalRevenue;                  // Tổng doanh thu
    long totalTransactions;               // Tổng số giao dịch thành công
    Map<String, Double> revenueByPackage; // Doanh thu theo gói: {"Gói VIP 1": 5000000, "Gói VIP 2": 12000000}

    // --- 3. Thống kê Dung lượng ---
    UserStorageResponse adminStorageStats; // Tổng dung lượng trong nhóm Admin/Hệ thống quản lý
    List<UserStorageDetailResponse> topUsersStorage; // Thống kê dung lượng người dùng (kèm Thumbnail)
}
