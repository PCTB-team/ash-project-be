package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.LoginRequest;
import com.pctb.webapp.dto.request.OtpRequest;
import com.pctb.webapp.dto.request.RegisterRequest;
import com.pctb.webapp.dto.request.VerifyOtpRequest;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}