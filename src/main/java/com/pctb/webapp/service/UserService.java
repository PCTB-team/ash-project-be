package com.pctb.webapp.service;

import com.pctb.webapp.dto.response.UserResponse;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.mapper.UserMapper;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class UserService {
    UserRepo userRepo;
    UserMapper userMapper;
    // Lấy toàn bộ User
    public List<UserResponse> getUser(){

        List<UserResponse> userResponses = userMapper.toUserResponseList(userRepo.findAll());

        return userResponses;
    }

}
