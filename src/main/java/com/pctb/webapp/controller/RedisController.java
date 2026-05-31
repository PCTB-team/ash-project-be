package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.RedisRequest;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.service.RedisService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/redis")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisController {
    RedisService redisService;

    @PostMapping("/set")
    public ApiResponse<Map<String, Object>> setValue(@RequestBody @Valid RedisRequest request) {
        redisService.setValue(request.getKey(), request.getValue(), request.getTtlSeconds());

        Map<String, Object> result = new HashMap<>();
        result.put("key", request.getKey());
        result.put("value", request.getValue());
        result.put("ttlSeconds", request.getTtlSeconds());

        return ApiResponse.<Map<String, Object>>builder()
                .message("Redis value saved successfully")
                .result(result)
                .build();
    }

    @GetMapping("/{key}")
    public ApiResponse<Map<String, Object>> getValue(@PathVariable String key) {
        String value = redisService.getValue(key);

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("value", value);

        return ApiResponse.<Map<String, Object>>builder()
                .message("Redis value retrieved successfully")
                .result(result)
                .build();
    }

    @DeleteMapping("/{key}")
    public ApiResponse<Map<String, Object>> deleteValue(@PathVariable String key) {
        redisService.deleteValue(key);

        Map<String, Object> result = new HashMap<>();
        result.put("key", key);
        result.put("deleted", true);

        return ApiResponse.<Map<String, Object>>builder()
                .message("Redis value deleted successfully")
                .result(result)
                .build();
    }
}
