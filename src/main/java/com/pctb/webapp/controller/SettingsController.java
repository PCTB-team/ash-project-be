package com.pctb.webapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.service.IntroSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SettingsController {
    IntroSettingsService introSettingsService;

    @Operation(summary = "Get intro page configuration")
    @GetMapping("/intro")
    public ApiResponse<JsonNode> getIntroSettings() {
        return ApiResponse.<JsonNode>builder()
                .message("Get intro settings successfully")
                .result(introSettingsService.getIntroSettings())
                .build();
    }

    @Operation(summary = "Update intro page configuration")
    @PutMapping("/intro")
    public ApiResponse<JsonNode> updateIntroSettings(@RequestBody JsonNode request) {
        return ApiResponse.<JsonNode>builder()
                .message("Intro settings updated successfully")
                .result(introSettingsService.updateIntroSettings(request))
                .build();
    }
}
