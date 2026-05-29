package com.pctb.webapp.controller;

import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.dto.response.UserResponse;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.service.UserService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
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
}
