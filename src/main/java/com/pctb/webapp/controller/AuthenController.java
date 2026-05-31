package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.LoginRequest;
import com.pctb.webapp.dto.request.LogoutRequest;
import com.pctb.webapp.dto.request.RegisterRequest;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.LoginResponse;
import com.pctb.webapp.dto.response.LogoutResponse;
import com.pctb.webapp.dto.response.RegisterResponse;
import com.pctb.webapp.service.AuthenService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenController {
    AuthenService authenService;

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

    @PostMapping("/logout")
    public ApiResponse<LogoutResponse> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @RequestBody @Valid LogoutRequest request
    ) {
        authenService.logout(authorizationHeader, request);

        return ApiResponse.<LogoutResponse>builder()
                .message("Logout successfully")
                .result(LogoutResponse.builder().loggedOut(true).build())
                .build();
    }
}
