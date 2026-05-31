package com.pctb.webapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RedisRequest {
    @NotBlank(message = "REDIS_KEY_INVALID")
    String key;

    @NotBlank(message = "REDIS_VALUE_INVALID")
    String value;

    @Positive(message = "REDIS_TTL_INVALID")
    Long ttlSeconds;
}
