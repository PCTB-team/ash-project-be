package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.AdminUpdateGroupStatusRequest;
import com.pctb.webapp.dto.request.AdminUpdatePlanRequest;
import com.pctb.webapp.dto.request.AdminUpdateSettingsRequest;
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

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminController {

    AdminService adminService;

    // ==========================================
    // 📊 1. TRANG 1: DASHBOARD TỔNG QUAN
    // ==========================================
    @Operation(summary = "Page 1: Get dashboard stat cards and real-time chart data")
    @GetMapping("/dashboard/stats")
    public ApiResponse<DashboardStatsResponse> getDashboardStats() {
        return ApiResponse.<DashboardStatsResponse>builder()
                .result(adminService.getDashboardStats())
                .build();
    }

    @Operation(summary = "Pages 1 and 2: Get categorized audit logs (ADMIN_ACTION, USER_ACTION, DOCUMENT_LOG)")
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
    // 👥 2. TRANG 2: QUẢN LÝ NGƯỜI DÙNG (USERS)
    // ==========================================
    @Operation(summary = "Page 2: Search, filter by role/status, and paginate users")
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

    @Operation(summary = "Page 2: Get detailed administrative user profile")
    @GetMapping("/users/{userId}")
    public ApiResponse<UserResponse> getUserDetail(@PathVariable String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(adminService.getUserDetailById(userId))
                .build();
    }

    @Operation(summary = "Page 2: Update account role (User/Admin)")
    @PutMapping("/users/{userId}/role")
    public ApiResponse<String> changeUserRole(
            @PathVariable String userId,
            @RequestParam String roleName,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String adminName = jwt != null ? jwt.getClaimAsString("sub") : "SystemAdmin";
        adminService.updateUserRole(userId, roleName, adminName);
        return ApiResponse.<String>builder()
                .message("User privilege updated successfully")
                .result("UPDATED")
                .build();
    }

    @Operation(summary = "Page 2: Lock a violating user account and write an audit log")
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

    @Operation(summary = "Page 2: Unlock a user account")
    @PutMapping("/users/{userId}/unlock")
    public ApiResponse<String> unlockUser(@PathVariable String userId, @AuthenticationPrincipal Jwt jwt) {
        String adminName = jwt != null ? jwt.getClaimAsString("sub") : "SystemAdmin";
        adminService.updateUserStatus(userId, true, null, adminName);
        return ApiResponse.<String>builder()
                .message("User account unlocked successfully")
                .result("ACTIVE")
                .build();
    }

    @Operation(summary = "Page 2: Permanently delete a violating user account")
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
    // 📚 3. TRANG 3: GIÁM SÁT TÀI LIỆU (DOCUMENTS)
    // ==========================================
    @Operation(summary = "Page 3: Monitor, search, and paginate documents")
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

    @Operation(summary = "Page 3: Move a policy-violating document to system trash")
    @DeleteMapping("/documents/{docId}")
    public ApiResponse<String> deleteDocument(@PathVariable String docId, @AuthenticationPrincipal Jwt jwt) {
        String adminName = jwt != null ? jwt.getClaimAsString("sub") : "SystemAdmin";
        adminService.softDeleteDocument(docId, adminName);
        return ApiResponse.<String>builder()
                .message("Document moved to system trash successfully")
                .result("MOVED_TO_TRASH")
                .build();
    }

    @Operation(summary = "Page 3: Get document summary statistics")
    @GetMapping("/documents/statistics")
    public ApiResponse<AdminDocumentStatsResponse> getDocumentStatsGrid() {
        return ApiResponse.<AdminDocumentStatsResponse>builder()
                .result(adminService.getDocumentGridStatistics())
                .build();
    }

    // ==========================================
    // 👥 4. TRANG 4: QUẢN LÝ NHÓM (STUDY GROUPS)
    // ==========================================
    @Operation(summary = "Page 4: Monitor, search, and paginate study groups")
    @GetMapping("/groups")
    public ApiResponse<Page<AdminGroupResponse>> getAllGroups(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.<Page<AdminGroupResponse>>builder()
                .result(adminService.getAllGroupsPaged(page, size, keyword))
                .build();
    }

    @Operation(summary = "Page 4: Get study group details by ID")
    @GetMapping("/groups/{groupId}")
    public ApiResponse<AdminGroupResponse> getGroupDetail(@PathVariable String groupId) {
        return ApiResponse.<AdminGroupResponse>builder()
                .result(adminService.getGroupDetail(groupId))
                .build();
    }

    @Operation(summary = "Page 4: Get study group statistics")
    @GetMapping("/groups/statistics")
    public ApiResponse<AdminGroupStatsResponse> getGroupStats() {
        return ApiResponse.<AdminGroupStatsResponse>builder()
                .result(adminService.getGroupStatistics())
                .build();
    }

    @Operation(summary = "Page 4: Update study group status")
    @PutMapping("/groups/{groupId}/status")
    public ApiResponse<String> updateGroupStatus(
            @PathVariable String groupId,
            @RequestBody AdminUpdateGroupStatusRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String adminName = jwt != null ? jwt.getClaimAsString("sub") : "SystemAdmin";
        String status = adminService.updateGroupStatus(groupId, request, adminName);
        return ApiResponse.<String>builder()
                .message("Study group status updated successfully")
                .result(status)
                .build();
    }

    // 🌟 ĐÃ ĐỒNG BỘ FIX LỖI: Bổ sung tham số `@AuthenticationPrincipal Jwt jwt` và đẩy `adminName` vào Service để lưu nhật ký Audit Log
    @Operation(summary = "Page 4: Permanently delete a policy-violating study group")
    @DeleteMapping("/groups/{groupId}")
    public ApiResponse<String> deleteGroup(@PathVariable String groupId, @AuthenticationPrincipal Jwt jwt) {
        String adminName = jwt != null ? jwt.getClaimAsString("sub") : "SystemAdmin";
        adminService.deleteGroup(groupId, adminName);
        return ApiResponse.<String>builder()
                .message("Study group dismantled successfully")
                .result("GROUP_DELETED")
                .build();
    }

    // ==========================================
    // 💳 5. TRANG 5: QUẢN LÝ THANH TOÁN (PAYMENTS)
    // ==========================================
    @Operation(summary = "Page 5: View and paginate user payment history")
    @GetMapping("/payments")
    public ApiResponse<Page<AdminTransactionResponse>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.<Page<AdminTransactionResponse>>builder()
                .result(adminService.getAllPaymentsPaged(page, size, status))
                .build();
    }

    @Operation(summary = "Page 5: Get storage plans for administration")
    @GetMapping("/plans")
    public ApiResponse<List<AdminPlanResponse>> getPlans() {
        return ApiResponse.<List<AdminPlanResponse>>builder()
                .message("Get plans successfully")
                .result(adminService.getPlans())
                .build();
    }

    @Operation(summary = "Page 5: Update a storage plan")
    @PutMapping("/plans/{planId}")
    public ApiResponse<AdminPlanResponse> updatePlan(
            @PathVariable String planId,
            @RequestBody AdminUpdatePlanRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String adminName = jwt != null ? jwt.getClaimAsString("sub") : "SystemAdmin";
        return ApiResponse.<AdminPlanResponse>builder()
                .message("Storage plan updated successfully")
                .result(adminService.updatePlan(planId, request, adminName))
                .build();
    }

    // ==========================================
    // 🤖 6. TRANG 6: THỐNG KÊ AI (AI STATS)
    // ==========================================
    @Operation(summary = "Page 6: Get AI chatbot usage statistics and charts")
    @GetMapping("/ai/statistics")
    public ApiResponse<AdminAiStatsResponse> getAiStats() {
        return ApiResponse.<AdminAiStatsResponse>builder()
                .result(adminService.getAiStatistics())
                .build();
    }

    // ==========================================
    // ⚙️ 7. TRANG 7: CÀI ĐẶT HỆ THỐNG
    // ==========================================
    @Operation(summary = "Page 7: Get administrative system settings")
    @GetMapping("/settings")
    public ApiResponse<SystemSettingsResponse> getSettings() {
        return ApiResponse.<SystemSettingsResponse>builder()
                .result(adminService.getSystemSettings())
                .build();
    }

    @Operation(summary = "Page 7: Update administrative system settings")
    @PutMapping("/settings")
    public ApiResponse<SystemSettingsResponse> updateSettings(
            @RequestBody AdminUpdateSettingsRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String adminName = jwt != null ? jwt.getClaimAsString("sub") : "SystemAdmin";
        return ApiResponse.<SystemSettingsResponse>builder()
                .message("System settings updated successfully")
                .result(adminService.updateSystemSettings(request, adminName))
                .build();
    }
}
