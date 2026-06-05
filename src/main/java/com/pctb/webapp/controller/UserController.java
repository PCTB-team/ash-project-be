package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.UpdateProfileRequest;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.UserProfileResponse;
import com.pctb.webapp.dto.response.UserResponse;
import com.pctb.webapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class UserController {
    UserService userService;

    // Đưa ra API
    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUser(){

        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getUser())
                .build();
    }

    // api lâý profile gọi userprofile.service
    // Lấy profile của user đang đăng nhập dựa trên userId nằm trong JWT.
    @Operation(summary = "Get current user profile", security = @SecurityRequirement(name = "bearerAuth"))
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

    // Cập nhật profile bằng multipart/form-data để nhận text field và file avatar.
    @Operation(summary = "Update current user profile", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UserProfileResponse> updateProfile(
            @Parameter(hidden = true) JwtAuthenticationToken authentication,
            @ModelAttribute @Valid UpdateProfileRequest request
    ) {
        String userId = authentication.getToken().getSubject();

        return ApiResponse.<UserProfileResponse>builder()
                .message("Update profile successfully")
                .result(userService.updateProfile(userId, request))
                .build();
    }

}
