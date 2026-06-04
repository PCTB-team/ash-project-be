package com.pctb.webapp.controller;

import com.pctb.webapp.dto.request.RedisRequest;
import com.pctb.webapp.dto.response.ApiResponse;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.service.RedisService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/redis")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisController {
    RedisService redisService;

    @PostMapping("/set")
    public ApiResponse<String> set(@RequestBody @Valid RedisRequest request) {

        redisService.set(request.getKey(), request.getValue());

        return ApiResponse.<String>builder()
                .message("Set key successfully")
                .result(request.getKey())
                .build();
    }

    @PostMapping("/set-with-ttl")
    public ApiResponse<String> setWithTtl(@RequestBody @Valid RedisRequest request) {

        validateTtl(request.getTtlSeconds());
        redisService.setWithTtl(request.getKey(), request.getValue(), request.getTtlSeconds());

        return ApiResponse.<String>builder()
                .message("Set key with ttl successfully")
                .result(request.getKey())
                .build();
    }

    @GetMapping("/get")
    public ApiResponse<String> get(@RequestParam(required = false) String key) {
        validateKey(key);

        return ApiResponse.<String>builder()
                .message("Get key successfully")
                .result(redisService.get(key))
                .build();
    }

    @DeleteMapping("/delete")
    public ApiResponse<Boolean> delete(@RequestParam(required = false) String key) {
        validateKey(key);
        boolean existed = redisService.hasKey(key);
        redisService.delete(key);

        return ApiResponse.<Boolean>builder()
                .message("Delete key successfully")
                .result(existed)
                .build();
    }

    @PostMapping("/increment")
    public ApiResponse<Long> increment(@RequestParam(required = false) String key) {
        validateKey(key);

        return ApiResponse.<Long>builder()
                .message("Increment key successfully")
                .result(redisService.increment(key))
                .build();
    }

    @PostMapping("/expire")
    public ApiResponse<Boolean> expire(
            @RequestParam(required = false) String key,
            @RequestParam(required = false) Long ttlSeconds) {
        validateKey(key);
        validateTtl(ttlSeconds);

        return ApiResponse.<Boolean>builder()
                .message("Set key ttl successfully")
                .result(redisService.expire(key, ttlSeconds))
                .build();
    }

    @GetMapping("/ttl")
    public ApiResponse<Long> ttl(@RequestParam(required = false) String key) {
        validateKey(key);

        return ApiResponse.<Long>builder()
                .message("Get key ttl successfully")
                .result(redisService.getTtl(key))
                .build();
    }

    private void validateKey(String key) {
        if (key == null || key.isBlank()) {
            throw new AppException(ErrorCode.KEY_REQUIRED);
        }
    }


    private void validateTtl(Long ttlSeconds) {
        if (ttlSeconds == null || ttlSeconds < 1) {
            throw new AppException(ErrorCode.TTL_INVALID);
        }
    }
}
