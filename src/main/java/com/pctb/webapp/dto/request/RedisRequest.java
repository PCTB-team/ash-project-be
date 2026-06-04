package com.pctb.webapp.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RedisRequest {
    @NotBlank(message = "KEY_REQUIRED")
    String key;
    @NotBlank(message = "VALUE_REQUIRED")
    String value;

    @Min(value = 1, message = "TTL_INVALID")
    Long ttlSeconds;
}
