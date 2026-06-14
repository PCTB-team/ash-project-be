package com.pctb.webapp.dto.response;

import com.pctb.webapp.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import lombok.Builder; // <-- THÊM DÒNG NÀY

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

//=================== Class quyết định đầu ra của USER ================
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class UserResponse {
     String id;
     String username;
     String fullname;
     String email;
     Set<Role> roles;

     boolean verified;
     boolean accountNonLocked;
     LocalDateTime lockedAt;
     String lockedReason;
     String lockedByAdmin;
}
