package com.pctb.webapp.service;

import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedisService {
    StringRedisTemplate redisTemplate;

    public void setValue(String key, String value, Long ttlSeconds) {
        if (ttlSeconds != null) {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
            return;
        }
        redisTemplate.opsForValue().set(key, value);
    }

    public String getValue(String key) {
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            throw new AppException(ErrorCode.REDIS_KEY_NOT_FOUND);
        }
        return value;
    }

    public void deleteValue(String key) {
        Boolean deleted = redisTemplate.delete(key);
        if (!Boolean.TRUE.equals(deleted)) {
            throw new AppException(ErrorCode.REDIS_KEY_NOT_FOUND);
        }
    }
}
