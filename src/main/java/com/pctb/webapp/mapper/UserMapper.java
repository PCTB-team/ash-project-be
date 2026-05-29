package com.pctb.webapp.mapper;

import com.pctb.webapp.dto.response.UserResponse;
import com.pctb.webapp.entity.User;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toUserResponse(User user);
    // Tự động chuyển list từ user sang list của userResponse
    List<UserResponse> toUserResponseList(List<User> userList);
}
