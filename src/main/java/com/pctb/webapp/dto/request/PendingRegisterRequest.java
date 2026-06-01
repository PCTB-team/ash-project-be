package com.pctb.webapp.dto.request;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)

// Class dùng để lưu tạm thông tin user vào redis để chờ xác thực
public class PendingRegisterRequest {
    String username;
    String email;
    String fullname;
    String encodedPassword;
}
