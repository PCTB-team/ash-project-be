package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.LockUserRequest;
import com.pctb.webapp.dto.response.*;
import com.pctb.webapp.entity.SystemLog;
import com.pctb.webapp.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin") // Tự động nhận diện context-path /api/v1 từ file cấu hình của nhóm
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminController {

    AdminService adminService;

    // ==========================================
    // 📊 1. CÁC API DÀNH CHO TRANG 1: DASHBOARD TỔNG QUAN
    // ==========================================
    @Operation(summary = "Trang 1: Lấy số liệu Stat Cards lớn và mảng đồ thị nạp Chart thời gian thực")
    @GetMapping("/dashboard/stats")
    public ApiResponse<DashboardStatsResponse> getDashboardStats() {
        return ApiResponse.<DashboardStatsResponse>builder()
                .result(adminService.getDashboardStats())
                .build();
    }

    @Operation(summary = "Trang 1 & 2: Xem nhật ký Audit Trail phân loại 3 phần rạch ròi (logType: ADMIN_ACTION, USER_ACTION, DOCUMENT_LOG)")
    @GetMapping("/logs")
    public ApiResponse<Page<SystemLog>> getSystemAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String logType
    ) {
        return ApiResponse.<Page<SystemLog>>builder()
                .result(adminService.getSystemAuditLogsPaged(page, size, logType))
                .build();
    }

    // ==========================================
    // 👥 2. CÁC API DÀNH CHO TRANG 2: QUẢN LÝ NGƯỜI DÙNG (USERS)
    // ==========================================
    @Operation(summary = "Trang 2: Tìm kiếm, lọc quyền, lọc trạng thái Active/Banned và phân trang User")
    @GetMapping("/users")
    public ApiResponse<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.<Page<UserResponse>>builder()
                .result(adminService.getAllUsersPaged(page, size, keyword, role, status))
                .build();
    }

    @Operation(summary = "Trang 2: Xem hồ sơ thông tin hành chính chi tiết của một sinh viên")
    @GetMapping("/users/{userId}")
    public ApiResponse<UserResponse> getUserDetail(@PathVariable String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(adminService.getUserDetailById(userId))
                .build();
    }

    @Operation(summary = "Trang 2: Cấp quyền / Thay đổi role tài khoản (User ↔ Admin)")
    @PutMapping("/users/{userId}/role")
    public ApiResponse<String> changeUserRole(
            @PathVariable String userId,
            @RequestParam String roleName,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String adminName = jwt != null ? jwt.getClaimAsString("sub") : "SystemAdmin";
        adminService.updateUserRole(userId, roleName, adminName);
        return ApiResponse.<String>builder()
                .message("User privilege updated to " + roleName.toUpperCase() + " successfully")
                .result("UPDATED")
                .build();
    }

    @Operation(summary = "Trang 2: Khóa tài khoản người dùng vi phạm (Banned) kèm lưu vết log")
    @PutMapping("/users/{userId}/lock")
    public ApiResponse<String> lockUser(
            @PathVariable String userId,
            @Valid @RequestBody LockUserRequest lockRequest,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String adminName = jwt != null ? jwt.getClaimAsString("sub") : "SystemAdmin";
        adminService.updateUserStatus(userId, false, lockRequest.getReason(), adminName);
        return ApiResponse.<String>builder()
                .message("User account banned successfully")
                .result("BANNED")
                .build();
    }

    @Operation(summary = "Trang 2: Mở khóa tài khoản người dùng hoạt động trở lại")
    @PutMapping("/users/{userId}/unlock")
    public ApiResponse<String> unlockUser(@PathVariable String userId, @AuthenticationPrincipal Jwt jwt) {
        String adminName = jwt != null ? jwt.getClaimAsString("sub") : "SystemAdmin";
        adminService.updateUserStatus(userId, true, null, adminName);
        return ApiResponse.<String>builder()
                .message("User account unlocked successfully")
                .result("ACTIVE")
                .build();
    }

    @Operation(summary = "Trang 2: Xóa vĩnh viễn tài khoản sinh viên vi phạm và cắt xé lịch sử ngoại")
    @DeleteMapping("/users/{userId}")
    public ApiResponse<String> deleteUser(@PathVariable String userId, @AuthenticationPrincipal Jwt jwt) {
        String adminName = jwt != null ? jwt.getClaimAsString("sub") : "SystemAdmin";
        adminService.deleteUserAccount(userId, adminName);
        return ApiResponse.<String>builder()
                .message("User record permanently purged from database")
                .result("DELETED")
                .build();
    }

    // ==========================================
    // 📚 3. CÁC API DÀNH CHO TRANG 3: QUẢN LÝ TÀI LIỆU (DOCUMENTS)
    // ==========================================

    // 🌟 SỬA ĐỒNG BỘ: Đón tiếp kiểu Page<AdminDocumentResponse> thay thế Page<Document> thô để giấu link Cloudinary bảo mật
    @Operation(summary = "Trang 3: Giám sát, tìm kiếm và phân trang tài liệu (Bảo mật: Giấu link fileUrl)")
    @GetMapping("/documents")
    public ApiResponse<Page<AdminDocumentResponse>> getAllDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String fileType
    ) {
        return ApiResponse.<Page<AdminDocumentResponse>>builder()
                .result(adminService.getAllDocumentsPaged(page, size, keyword, fileType))
                .build();
    }

    @Operation(summary = "Trang 3: Admin hạ gỡ và xóa mềm tài liệu vi phạm quy chế StudyHub")
    @DeleteMapping("/documents/{docId}")
    public ApiResponse<String> deleteDocument(@PathVariable String docId, @AuthenticationPrincipal Jwt jwt) {
        String adminName = jwt != null ? jwt.getClaimAsString("sub") : "SystemAdmin";
        adminService.softDeleteDocument(docId, adminName);
        return ApiResponse.<String>builder()
                .message("Document moved to system trash successfully")
                .result("MOVED_TO_TRASH")
                .build();
    }

    @Operation(summary = "Trang 3: Lấy số liệu lưới nhỏ trên đỉnh bảng (Tổng dung lượng, tệp lớn nhất, uploader đỉnh cao)")
    @GetMapping("/documents/statistics")
    public ApiResponse<AdminDocumentStatsResponse> getDocumentStatsGrid() {
        return ApiResponse.<AdminDocumentStatsResponse>builder()
                .result(adminService.getDocumentGridStatistics())
                .build();
    }
}