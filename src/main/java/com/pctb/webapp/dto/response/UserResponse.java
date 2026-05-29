package com.pctb.webapp.dto.response;

import com.pctb.webapp.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

//=================== Class quyết định đầu ra của USER ================
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserResponse {
     String id;
     String username;
     String fullname;
     String email;
     Set<Role> roles;
}
