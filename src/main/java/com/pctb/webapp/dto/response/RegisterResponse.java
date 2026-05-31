package com.pctb.webapp.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
// ====================== Format response khi vào controller ==================
public class RegisterResponse {
    String fullname;
    String email;
    String username;

}
