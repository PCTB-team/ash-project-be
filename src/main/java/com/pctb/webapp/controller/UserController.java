package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.UpdateProfileRequest;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.UserProfileResponse;
import com.pctb.webapp.dto.response.UserResponse;
import com.pctb.webapp.dto.response.UserStorageResponse;
import com.pctb.webapp.entity.StoragePlan;
import com.pctb.webapp.service.PaymentService;
import com.pctb.webapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class UserController {
    UserService userService;
    private final PaymentService paymentService;
    // Đưa ra API
    @Operation(summary = "Get all users")
    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUser(){

        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getUser())
                .build();
    }

    // api lâý profile gọi userprofile.service
    // Lấy profile của user đang đăng nhập dựa trên userId nằm trong JWT.
    @Operation(summary = "Get current user profile")
    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getProfile(
            @Parameter(hidden = true) JwtAuthenticationToken authentication
    ) {
        String userId = authentication.getToken().getSubject();

        return ApiResponse.<UserProfileResponse>builder()
                .message("Get profile successfully")
                .result(userService.getProfile(userId))
                .build();
    }

    @Operation(summary = "Get current user storage usage")
    @GetMapping("/storage")
    public ApiResponse<UserStorageResponse> getStorage(
            @Parameter(hidden = true) JwtAuthenticationToken authentication
    ) {
        String userId = authentication.getToken().getSubject();

        return ApiResponse.<UserStorageResponse>builder()
                .message("Get storage usage successfully")
                .result(userService.getStorage(userId))
                .build();
    }

    // Cập nhật profile bằng multipart/form-data để nhận text field và file avatar.
    @Operation(summary = "Update current user profile")
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserProfileResponse> updateProfile(
            @Parameter(hidden = true) JwtAuthenticationToken authentication,
            @ModelAttribute @Valid UpdateProfileRequest request,
            @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        String userId = authentication.getToken().getSubject();
        if ((request.getAvatar() == null || request.getAvatar().isEmpty()) && file != null && !file.isEmpty()) {
            request.setAvatar(file);
        }

        return ApiResponse.<UserProfileResponse>builder()
                .message("Update profile successfully")
                .result(userService.updateProfile(userId, request))
                .build();
    }

    @Operation(summary = "Get all available storage plans for upgrade")
    @GetMapping("/available-plans")
    public ApiResponse<List<StoragePlan>> getAvailablePlans() {
        return ApiResponse.<List<StoragePlan>>builder()
                .message("Get available plans successfully")
                .result(paymentService.getAllAvailablePlans()) // Gọi sang hàm service của bạn
                .build();
    }

}
