package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.RegisterRequest;
import com.pctb.webapp.dto.response.RegisterResponse;
import com.pctb.webapp.entity.Role;
import com.pctb.webapp.entity.RoleEnum;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.repository.RoleRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenService {
    UserRepo userRepo;
    RoleRepo roleRepo;
    PasswordEncoder passwordEncoder;
// fuction đăng kí truyền vào request
    public RegisterResponse register(RegisterRequest request){
        // kiểm tra xem email có tồn tại chưa
        if (userRepo.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        // kiểm tra username có tồn tại chưa
        if (userRepo.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
        }
        // kiểm tra password có bằng confirmpassword chưa
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new AppException(ErrorCode.CONFIRM_PASSWORD_NOT_MATCH);
        }
        // Tạo role User nếu dưới DataBase chưa có
        Role userRole = roleRepo.findById(RoleEnum.USER.name())
                .orElseGet(() -> roleRepo.save(new Role(RoleEnum.USER.name(), "User role")));
        // giá trị user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullname(request.getFullname())
                .roles(Set.of(userRole)) // sẽ tạo nhanh 1 Set rồi add role vào
                .build();
        userRepo.save(user);

        return RegisterResponse.builder()
                .fullname(user.getFullname())
                .email(user.getEmail())
                .username(user.getUsername())
                .build();
    }
}
