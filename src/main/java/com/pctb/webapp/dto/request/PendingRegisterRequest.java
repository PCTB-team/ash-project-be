package com.pctb.webapp.dto.request;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PendingRegisterRequest {
    String username;
    String email;
    String fullname;
    String encodedPassword;
}
