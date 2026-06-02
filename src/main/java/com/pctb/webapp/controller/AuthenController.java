package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.*;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.LoginResponse;
import com.pctb.webapp.dto.response.OtpResponse;
import com.pctb.webapp.dto.response.RegisterResponse;
import com.pctb.webapp.service.AuthenService;
import com.pctb.webapp.service.OtpService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenController {
    AuthenService authenService;
    OtpService otpService;

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
    public ApiResponse<Map<String, String>> sendOtpForgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authenService.sendOtpForgotPassword(request);

        return ApiResponse.<Map<String, String>>builder()
                .message("Verification OTP has been successfully dispatched to your email address.")
                .result(Map.of("email", request.getEmail()))
                .build();
    }

    @PostMapping("/forgot-password/verify-otp")
    public ApiResponse<Map<String, String>> verifyOtpForgotPassword(
            @Valid @RequestBody VerifyOtpRequest request) {

        String resetToken =
                authenService.verifyOtpForgotPassword(request);

        return ApiResponse.<Map<String, String>>builder()
                .message("OTP verification successful.")
                .result(Map.of("resetToken", resetToken))
                .build();
    }

    @PostMapping("/forgot-password/reset")
    public ApiResponse<Object> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authenService.resetPassword(request);

        return ApiResponse.builder()
                .message("Password reset successfully.")
                .result(null)
                .build();
    }

    @PostMapping("/forgot-password/resend-otp")
    public ApiResponse<Map<String, String>> resendForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authenService.resendForgotPasswordOtp(request);

        return ApiResponse.<Map<String, String>>builder()
                .message("OTP resent successfully")
                .result(Map.of("email", request.getEmail()))
                .build();
    }
}
