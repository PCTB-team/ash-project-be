package com.pctb.webapp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import com.pctb.webapp.dto.request.LoginRequest;
import com.pctb.webapp.dto.request.LogoutRequest;
import com.pctb.webapp.dto.request.OtpRequest;
import com.pctb.webapp.dto.request.RefreshTokenRequest;
import com.pctb.webapp.dto.request.RegisterRequest;
import com.pctb.webapp.dto.request.TokenTestRequest;
import com.pctb.webapp.dto.request.VerifyOtpRequest;
import com.pctb.webapp.dto.request.*;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.LoginResponse;
import com.pctb.webapp.dto.response.LogoutResponse;
import com.pctb.webapp.dto.response.OtpResponse;
import com.pctb.webapp.dto.response.RefreshTokenTestResponse;
import com.pctb.webapp.dto.response.RegisterResponse;
import com.pctb.webapp.dto.response.TokenInfoResponse;
import com.pctb.webapp.service.AuthenService;
import com.pctb.webapp.service.OtpService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenController {
    AuthenService authenService;
    OtpService otpService;

    @Operation(summary = "Register new account")
    @PostMapping("/register") //
    public ApiResponse<RegisterResponse> register(@RequestBody @Valid RegisterRequest request) {
        return ApiResponse.<RegisterResponse>builder()
                .message("Register successfully. Please verify your email")
                .result(authenService.register(request))
                .build();
    }

    @Operation(summary = "Login account")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.<LoginResponse>builder()
                .message("Login successfully")
                .result(authenService.login(request))
                .build();
    }

    @Operation(summary = "Refresh access token")
    @PostMapping("/refresh-token")
    public ApiResponse<LoginResponse> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        return ApiResponse.<LoginResponse>builder()
                .message("Token refreshed successfully")
                .result(authenService.refreshToken(request))
                .build();
    }

    @Operation(summary = "Test new access token")
    @PostMapping("/test-new-access-token")
    public ApiResponse<RefreshTokenTestResponse> testNewAccessToken(@RequestBody @Valid RefreshTokenRequest request) {
        return ApiResponse.<RefreshTokenTestResponse>builder()
                .message("New access token is valid")
                .result(authenService.testNewAccessToken(request))
                .build();
    }

    @Operation(summary = "Logout account")
    @PostMapping("/logout")
    public ApiResponse<LogoutResponse> logout(
            @RequestBody @Valid LogoutRequest request,
            @Parameter(hidden = true) JwtAuthenticationToken authentication
    ) {
        return ApiResponse.<LogoutResponse>builder()
                .message("Logout successfully")
                .result(authenService.logout(request, authentication.getToken().getTokenValue()))
                .build();
    }

    @Operation(summary = "Test access or refresh token")
    @PostMapping("/test-token")
    public ApiResponse<TokenInfoResponse> testToken(@RequestBody @Valid TokenTestRequest request) {
        return ApiResponse.<TokenInfoResponse>builder()
                .message("Token is valid")
                .result(authenService.testToken(request.getToken()))
                .build();
    }

    @Operation(summary = "Resend registration OTP")
    @PostMapping("/otp-requests")
    public ApiResponse<OtpResponse> resendOtp(@RequestBody @Valid OtpRequest request) {
        return ApiResponse.<OtpResponse>builder()
                .message("OTP resent successfully")
                .result(otpService.resendOtp(request.getEmail()))
                .build();
    }

    @Operation(summary = "Verify registration OTP")
    @PostMapping("/otp-verification")
    public ApiResponse<OtpResponse> verifyOtp(@RequestBody @Valid VerifyOtpRequest request) {
        return ApiResponse.<OtpResponse>builder()
                .message("Account verified successfully")
                .result(otpService.verifyOtp(request.getEmail(), request.getOtp()))
                .build();
    }
    @Operation(summary = "Send forgot password OTP")
    @PostMapping("/forgot-password/send-otp")
    public ApiResponse<Map<String, String>> sendOtpForgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authenService.sendOtpForgotPassword(request);

        return ApiResponse.<Map<String, String>>builder()
                .message("Verification OTP has been successfully dispatched to your email address.")
                .result(Map.of("email", request.getEmail()))
                .build();
    }

    @Operation(summary = "Verify forgot password OTP")
    @PostMapping("/forgot-password/verify-otp")
    public ApiResponse<Map<String, String>> verifyOtpForgotPassword(
            @Valid @RequestBody VerifyForgotPasswordOtpRequest request) {

        String resetToken =
                authenService.verifyOtpForgotPassword(request);

        return ApiResponse.<Map<String, String>>builder()
                .message("OTP verification successful.")
                .result(Map.of("resetToken", resetToken))
                .build();
    }

    @Operation(summary = "Reset password")
    @PostMapping("/forgot-password/reset")
    public ApiResponse<Object> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        authenService.resetPassword(request);

        return ApiResponse.builder()
                .message("Password reset successfully.")
                .result(null)
                .build();
    }

    @Operation(summary = "Resend forgot password OTP")
    @PostMapping("/forgot-password/resend-otp")
    public ApiResponse<Map<String, String>> resendForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordRequest request) {

        authenService.resendForgotPasswordOtp(request);

        return ApiResponse.<Map<String, String>>builder()
                .message("OTP resent successfully")
                .result(Map.of("email", request.getEmail()))
                .build();
    }

    @Operation(summary = "Login with Google")
    @PostMapping("/google-login")
    public ApiResponse<LoginResponse> googleLogin(@RequestBody @Valid GoogleLoginRequest request) {
        return ApiResponse.<LoginResponse>builder()
                .message("Google login successfully")
                .result(authenService.googleLogin(request))
                .build();
    }
}
