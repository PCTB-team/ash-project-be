package com.pctb.webapp.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate; // 🟢 Đã đổi thành LocalDate cho đồng bộ với Entity của nhóm

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoginLogResponse {
    String id;
    String username;
    String email;
    LocalDate loginDate; // 🟢 Đồng bộ chuẩn xác kiểu dữ liệu ngày
}