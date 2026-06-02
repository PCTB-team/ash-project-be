package com.pctb.webapp.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TokenInfoResponse {
    Boolean valid;
    String userId;
    String username;
    String email;
    String fullname;
    Boolean verified;
    List<String> roles;
    String tokenType;
    String issuer;
    String jwtId;
    String issuedAt;
    String expiresAt;
}
