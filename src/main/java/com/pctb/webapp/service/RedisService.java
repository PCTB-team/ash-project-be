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

    // Lưu key-value vào Redis không kèm thời gian hết hạn, dùng cho dữ liệu cache cần giữ đến khi xóa thủ công.
    public void set(String key, String value) {
        stringRedisTemplate.opsForValue().set(key, value);
    }

    // Lưu key-value vào Redis kèm TTL tính theo giây, phù hợp cho OTP, token tạm và các bộ đếm giới hạn.
    public void setWithTtl(String key, String value, long ttlSeconds) {
        stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
    }

    // Đọc value theo key; trả về null nếu key không tồn tại hoặc đã hết hạn.
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    // Kiểm tra một key có đang tồn tại trong Redis hay không.
    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
    }

    // Xóa key khỏi Redis, thường dùng sau khi OTP đã xác thực hoặc user logout.
    public void delete(String key) {
        stringRedisTemplate.delete(key);
    }

    // Tăng giá trị số của key lên 1, dùng để đếm số lần gửi OTP hoặc đăng nhập sai.
    public Long increment(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

    // Gán thời gian hết hạn mới cho key đã tồn tại.
    public boolean expire(String key, long ttlSeconds) {
        return Boolean.TRUE.equals(stringRedisTemplate.expire(key, Duration.ofSeconds(ttlSeconds)));
    }

    // Lấy TTL còn lại của key theo giây; kết quả âm nghĩa là key không tồn tại hoặc không có TTL.
    public Long getTtl(String key) {
        return stringRedisTemplate.getExpire(key);
    }
}
