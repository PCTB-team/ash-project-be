package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.*;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.LoginResponse;
import com.pctb.webapp.dto.response.OtpResponse;
import com.pctb.webapp.dto.response.RegisterResponse;
import com.pctb.webapp.service.AuthenService;
import com.pctb.webapp.service.ForgotPasswordService;
import com.pctb.webapp.service.OtpService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenController {
    AuthenService authenService;
    OtpService otpService;
    ForgotPasswordService forgotPasswordService;

    @PostMapping("/register")
    public ApiResponse<RegisterResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ApiResponse.<RegisterResponse>builder()
                .message("Register successfully. Please verify your email")
                .result(authenService.register(request))
                .build();
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.<LoginResponse>builder()
                .message("Login successfully")
                .result(authenService.login(request))
                .build();
    }

    @PostMapping("/otp-requests")
    public ApiResponse<OtpResponse> resendOtp(@RequestBody @Valid OtpRequest request) {
        return ApiResponse.<OtpResponse>builder()
                .message("OTP resent successfully")
                .result(otpService.resendOtp(request.getEmail()))
                .build();
    }

    @PostMapping("/otp-verification")
    public ApiResponse<OtpResponse> verifyOtp(@RequestBody @Valid VerifyOtpRequest request) {
        return ApiResponse.<OtpResponse>builder()
                .message("Account verified successfully")
                .result(otpService.verifyOtp(request.getEmail(), request.getOtp()))
                .build();
    }
    @PostMapping("/forgot-password/send-otp")
    @Operation(summary = "Step 1: Validate target Email and request OTP code dispatch")
    public ApiResponse<Map<String, String>> sendOtpForgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        forgotPasswordService.sendOtpForgotPassword(request);
        return ApiResponse.<Map<String, String>>builder()
                .message("Verification OTP has been successfully dispatched to your email address.")
                .result(Map.of("email", request.getEmail()))
                .build();
    }

    @PostMapping("/forgot-password/verify-otp")
    @Operation(summary = "Step 2: Verify submitted OTP values and acquire unique Reset Token transaction key")
    public ApiResponse<Map<String, String>> verifyOtpForgotPassword(@Valid @RequestBody VerifyOtpRequest request) {
        // Gọi hàm verifyOtpForgotPassword chuyên biệt của ForgotPasswordService để lấy resetToken
        String resetToken = forgotPasswordService.verifyOtpForgotPassword(request);
        return ApiResponse.<Map<String, String>>builder()
                .message("OTP verification successful. Use the provided reset token to update your password.")
                .result(Map.of("resetToken", resetToken))
                .build();
    }

    @PostMapping("/forgot-password/reset")
    @Operation(summary = "Step 3: Modify user password record using validated Reset Token credential passport")
    public ApiResponse<Object> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        forgotPasswordService.resetPassword(request);
        return ApiResponse.builder()
                .message("Password reset operation completed successfully. You may now perform authentication with your new password.")
                .result(null)
                .build();
    }
}
