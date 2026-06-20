package com.pctb.webapp.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserStorageDetailResponse {
    String username;
    String fullname;
    String thumbnailUrl; // Đường dẫn ảnh đại diện / thâm neo của user
    UserStorageResponse storage; // Re-use lại DTO dung lượng của bạn
}