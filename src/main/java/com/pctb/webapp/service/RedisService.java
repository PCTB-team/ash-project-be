package com.pctb.webapp.service;

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
    StringRedisTemplate stringRedisTemplate;
// lưu key và value không giới hạn
    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }
// lưu key và vlue có giới hạn Ttl
    public void setWithTtl(String key, String value, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
    }
// đọc value theo key
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }
// kiểm tra key có tồn tại không
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }
// xóa key
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }
// tăng giá trị của key
    public Long increment(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }
// gán thời gian hết hạn cho 1 key
    public boolean expire(String key, long ttlSeconds) {
        return Boolean.TRUE.equals(stringRedisTemplate.expire(key, Duration.ofSeconds(ttlSeconds)));
    }
// lấy thời gian sống còn lại của keyt
    public Long getTtl(String key) {
        return stringRedisTemplate.getExpire(key);
    }
}
