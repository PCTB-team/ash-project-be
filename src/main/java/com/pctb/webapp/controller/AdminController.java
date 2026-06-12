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

    // Xem danh sách và tìm kiếm User
    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(adminService.getAllAndSearchUsers(keyword, page, size));
    }

    // Tạm khóa User kèm truyền lý do
    @PutMapping("/users/{id}/lock")
    public ResponseEntity<String> lockUser(
            @PathVariable String id,
            @Valid @RequestBody LockUserRequest request,
            JwtAuthenticationToken token) {
        String adminUsername = token.getName();
        adminService.lockUser(id, request, adminUsername);
        // Đổi thông báo thành công sang tiếng Anh
        return ResponseEntity.ok("User account has been locked successfully and the action has been logged.");
    }

    // Mở khóa User
    @PutMapping("/users/{id}/unlock")
    public ResponseEntity<String> unlockUser(
            @PathVariable String id,
            JwtAuthenticationToken token) {
        String adminUsername = token.getName();
        adminService.unlockUser(id, adminUsername);
        // Đổi thông báo thành công sang tiếng Anh
        return ResponseEntity.ok("User account has been unlocked successfully.");
    }

    // Xem nhật ký hệ thống (System Logs)
    @GetMapping("/logs")
    public ResponseEntity<Page<SystemLog>> getLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminService.getSystemLogs(page, size));
    }

    // Xem thống kê hệ thống (Dashboard Stats)
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(adminService.getSystemStats());
    }

    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats() {
        return ResponseEntity.ok(ApiResponse.<DashboardStatsResponse>builder()
                .result(adminService.getDashboardStats())
                .build());
    }
}