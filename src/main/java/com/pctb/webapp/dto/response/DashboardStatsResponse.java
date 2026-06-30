package com.pctb.webapp.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardStatsResponse {
    // 👥 1. NHÓM USER
    long totalUsers;
    long activeUsersRightNow;
    long newUsersThisMonth;
    long activeUsers;

    // 💳 2. NHÓM DOANH THU (PAYOS)
    double totalRevenue;
    double revenueThisMonth;

    // 💾 3. NHÓM HẠ TẦNG LƯU TRỮ (STORAGE)
    long totalStorageUsedBytes;
    long totalStorageCapacityBytes;
    Map<String, Long> fileTypeDistribution; // {"PDF": 420, "IMAGE": 150, ...}

    // 📚 4. NHÓM TƯƠNG TÁC (ENGAGEMENT)
    long totalDocuments;
    long newDocsThisMonth;
    long totalGroups; // 🌟 Thêm đếm tổng nhóm theo yêu cầu Stat Card số 3 của FE
    long pendingReports;

    // 📈 DỮ LIỆU ĐỒ THỊ CHUYÊN SÂU THEO THỜI GIAN (Dành cho LineChart & AreaChart của FE)
    List<MonthlyStatItem> monthlyUserGrowth; // Mảng 6 tháng gần nhất vẽ Line Chart
    List<MonthlyStatItem> monthlyRevenueTrend; // Mảng 6 tháng gần nhất vẽ Area Chart

    List<WeeklyStatItem> weeklyUploadTrend;
    List<RecentActivityResponse> recentActivities;
}
