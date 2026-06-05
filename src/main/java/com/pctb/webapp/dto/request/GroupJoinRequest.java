package com.pctb.webapp.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GroupJoinRequest {
    @NotBlank(message = "Join code cannot be blank")
    String joinCode;

    // === THÊM TRƯỜNG NÀY ĐỂ USER NHẬP PASS KHI VÀO NHÓM PRIVATE ===
    String password;
}