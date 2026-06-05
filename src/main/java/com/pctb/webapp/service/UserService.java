package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.UpdateProfileRequest;
import com.pctb.webapp.dto.response.UserProfileResponse;
import com.pctb.webapp.dto.response.UserResponse;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.mapper.UserMapper;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
public class UserService {
    UserRepo userRepo;
    UserMapper userMapper;
    LocalStorageService localStorageService;
    PasswordEncoder passwordEncoder;
    // Lấy toàn bộ User
    public List<UserResponse> getUser(){

        List<UserResponse> userResponses = userMapper.toUserResponseList(userRepo.findAll());

        return userResponses;
    }
// backend lấy đúng user đang đăng nhập trề
    // Lấy profile của đúng user đang đăng nhập, không cho client tự truyền userId.
    public UserProfileResponse getProfile(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toUserProfileResponse(user);
    }

    // Luồng chính của update profile: validate dữ liệu, lưu avatar nếu có, đổi password nếu có, rồi lưu DB.
    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        validateFullname(request.getFullname());

        String oldAvatarUrl = user.getAvatarUrl();
        String newAvatarUrl = localStorageService.saveAvatar(userId, request.getAvatar());

        user.setFullname(request.getFullname().trim());
        user.setSchool(normalizeOptionalText(request.getSchool()));

        if (newAvatarUrl != null) {
            user.setAvatarUrl(newAvatarUrl);
        }

        updatePasswordIfRequested(user, request);

        try {
            User savedUser = userRepo.save(user);

            if (newAvatarUrl != null) {
                localStorageService.deleteAvatar(oldAvatarUrl);
            }

            return userMapper.toUserProfileResponse(savedUser);
        } catch (RuntimeException exception) {
            localStorageService.deleteAvatar(newAvatarUrl);
            throw exception;
        }
    }

    // Kiểm tra họ tên bắt buộc nhập và không vượt quá giới hạn lưu trong DB.
    private void validateFullname(String fullname) {
        if (fullname == null || fullname.isBlank() || fullname.length() > 100) {
            throw new AppException(ErrorCode.PROFILE_FULLNAME_INVALID);
        }
    }

    // Chuẩn hóa field optional: bỏ khoảng trắng dư, nếu rỗng thì lưu null.
    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    // Chỉ đổi password khi user nhập ít nhất một field password trong form update profile.
    private void updatePasswordIfRequested(User user, UpdateProfileRequest request) {
        if (!isPasswordChangeRequested(request)) {
            return;
        }

        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();
        String confirmPassword = request.getConfirmPassword();

        if (!hasText(oldPassword) || !passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new AppException(ErrorCode.OLD_PASSWORD_INCORRECT);
        }

        validateNewPassword(newPassword);

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new AppException(ErrorCode.NEW_PASSWORD_SAME_AS_OLD);
        }

        if (!hasText(confirmPassword) || !newPassword.equals(confirmPassword)) {
            throw new AppException(ErrorCode.PROFILE_CONFIRM_PASSWORD_NOT_MATCH);
        }

        user.setPassword(passwordEncoder.encode(newPassword));
    }

    // Xác định user có đang yêu cầu đổi password hay chỉ cập nhật fullname/school/avatar.
    private boolean isPasswordChangeRequested(UpdateProfileRequest request) {
        return hasText(request.getOldPassword())
                || hasText(request.getNewPassword())
                || hasText(request.getConfirmPassword());
    }

    // Kiểm tra password mới: tối thiểu 8 ký tự và có ít nhất 1 ký tự đặc biệt.
    private void validateNewPassword(String newPassword) {
        if (!hasText(newPassword)
                || newPassword.length() < 8
                || !newPassword.matches(".*[^a-zA-Z0-9].*")) {
            throw new AppException(ErrorCode.PROFILE_PASSWORD_INVALID);
        }
    }

    // Helper kiểm tra chuỗi có nội dung thật sau khi bỏ khoảng trắng.
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }


}
