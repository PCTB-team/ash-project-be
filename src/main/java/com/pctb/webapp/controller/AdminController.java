package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.LockUserRequest;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.DashboardStatsResponse;
import com.pctb.webapp.dto.response.UserResponse;
import com.pctb.webapp.entity.SystemLog;
import com.pctb.webapp.service.AdminService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminController {
    AdminService adminService;

    // Xem danh sách và tìm kiếm User (Bọc ApiResponse để đồng bộ dữ liệu trả về cho Front-end)
    @GetMapping("/users")
    public ApiResponse<Page<UserResponse>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.<Page<UserResponse>>builder()
                .result(adminService.getAllAndSearchUsers(keyword, page, size))
                .build();
    }

    // Tạm khóa User kèm truyền lý do
    @PutMapping("/users/{id}/lock")
    public ApiResponse<String> lockUser(
            @PathVariable String id,
            @Valid @RequestBody LockUserRequest request,
            JwtAuthenticationToken token) {
        String adminUsername = token.getName(); // Lấy username an toàn
        adminService.lockUser(id, request, adminUsername);

        return ApiResponse.<String>builder()
                .result("User account has been locked successfully and the action has been logged.")
                .build();
    }

    // Mở khóa User
    @PutMapping("/users/{id}/unlock")
    public ApiResponse<String> unlockUser(
            @PathVariable String id,
            JwtAuthenticationToken token) {
        String adminUsername = token.getName(); // Lấy username an toàn
        adminService.unlockUser(id, adminUsername);

        return ApiResponse.<String>builder()
                .result("User account has been unlocked successfully.")
                .build();
    }

    // Xem nhật ký hệ thống (System Logs)
    @GetMapping("/logs")
    public ApiResponse<Page<SystemLog>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<Page<SystemLog>>builder()
                .result(adminService.getSystemLogs(page, size))
                .build();
    }

    // Xem thống kê hệ thống cũ (Simple stats)
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        return ApiResponse.<Map<String, Object>>builder()
                .result(adminService.getSystemStats())
                .build();
    }

    // Xem thống kê hệ thống nâng cao (Bậc thang gói cước + Dung lượng Admin + Giao dịch thành công)
    @GetMapping("/dashboard/stats")
    public ApiResponse<DashboardStatsResponse> getDashboardStats() {
        return ApiResponse.<DashboardStatsResponse>builder()
                .result(adminService.getDashboardStats())
                .build();
    }

    // =========================================================================
    // ĐÃ ĐỒNG BỘ: ADMIN CHỦ ĐỘNG HỦY GÓI DUNG LƯỢNG VIP CỦA USER
    // =========================================================================
    @PutMapping("/users/{userId}/cancel-plan")
    public ApiResponse<String> cancelPlan(
            @PathVariable String userId,
            JwtAuthenticationToken token // Sửa đổi tại đây: Đồng bộ hóa dùng chung một loại Token với luồng Lock/Unlock
    ) {
        // Lấy chính xác tên định danh của admin đang thực hiện thao tác
        String adminUsername = token.getName();

        // Gọi service xử lý đẩy user về gói ban đầu
        adminService.cancelUserStoragePlan(userId, adminUsername);

        return ApiResponse.<String>builder()
                .result("Đã hủy gói nâng cấp của người dùng thành công. Tài khoản đã quay về mức 500MB.")
                .build();
    }
}