package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.LockUserRequest;
import com.pctb.webapp.dto.response.AdminTransactionResponse;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.UserResponse;
import com.pctb.webapp.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/page")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AdminController {

    AdminService adminService;

    @Operation(summary = "Search users by name, email, or username globally")
    @GetMapping("/users/search")
    public ApiResponse<List<UserResponse>> searchUsers(@RequestParam(required = false) String keyword) {
        return ApiResponse.<List<UserResponse>>builder()
                .result(adminService.searchUsers(keyword))
                .build();
    }

    @Operation(summary = "Retrieve payment transaction logs from PayOS (Filter by status: SUCCESS, PENDING, FAILED)")
    @GetMapping("/transactions/logs")
    public ApiResponse<List<AdminTransactionResponse>> getTransactionLogs(@RequestParam(required = false) String status) {
        return ApiResponse.<List<AdminTransactionResponse>>builder()
                .result(adminService.getTransactionLogs(status))
                .build();
    }

    @Operation(summary = "Action: Lock a specific user account due to violations")
    @PutMapping("/users/{userId}/lock")
    public ApiResponse<String> lockUser(
            @PathVariable String userId,
            @RequestBody @Valid LockUserRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        String adminUsername = jwt.getClaimAsString("sub");
        adminService.lockUser(userId, request, adminUsername);
        return ApiResponse.<String>builder()
                .message("User account locked successfully")
                .result("LOCKED")
                .build();
    }

    @Operation(summary = "Action: Unlock a specific user account")
    @PutMapping("/users/{userId}/unlock")
    public ApiResponse<String> unlockUser(@PathVariable String userId) {
        adminService.unlockUser(userId);
        return ApiResponse.<String>builder()
                .message("User account unlocked successfully")
                .result("UNLOCKED")
                .build();
    }

    @Operation(summary = "Action: Cancel VIP privileges and downgrade storage quota to default 500MB")
    @DeleteMapping("/users/{userId}/cancel-plan")
    public ApiResponse<String> cancelPlan(@PathVariable String userId) {
        adminService.cancelUserVipPlan(userId);
        return ApiResponse.<String>builder()
                .message("VIP privileges cancelled successfully, storage quota reset to 500MB")
                .result("CANCELLED")
                .build();
    }

    @Operation(summary = "Retrieve account security audit logs (Filter by type: LOCKED, NEW, combined with specific keyword search)")
    @GetMapping("/logs/accounts")
    public ApiResponse<List<UserResponse>> getAccountAuditLogs(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword
    ) {
        return ApiResponse.<List<UserResponse>>builder()
                .result(adminService.getAccountAuditLogs(type, keyword))
                .build();
    }

    @Operation(summary = "Retrieve real-time user login history logs across the system")
    @GetMapping("/logs/logins")
    public ApiResponse<?> getLoginLogs() {
        return ApiResponse.builder()
                .result(adminService.getLoginLogs())
                .build();
    }
}