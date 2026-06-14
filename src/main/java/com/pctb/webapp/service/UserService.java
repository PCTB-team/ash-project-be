package com.pctb.webapp.service;

import com.pctb.webapp.dto.request.UpdateProfileRequest;
import com.pctb.webapp.dto.response.UserProfileResponse;
import com.pctb.webapp.dto.response.UserResponse;
import com.pctb.webapp.entity.User;
import com.pctb.webapp.exception.AppException;
import com.pctb.webapp.exception.ErrorCode;
import com.pctb.webapp.mapper.UserMapper;
import com.pctb.webapp.repository.DocumentRepo;
import com.pctb.webapp.repository.UserLoginHistoryRepo;
import com.pctb.webapp.repository.UserRepo;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserService {
    UserRepo userRepo;
    DocumentRepo documentRepo;
    UserLoginHistoryRepo userLoginHistoryRepo;
    UserMapper userMapper;
    CloudinaryStorageService CloudinaryStorageService;
    PasswordEncoder passwordEncoder;

    // Lấy toàn bộ user trong hệ thống và chuyển sang DTO để tránh trả trực tiếp entity.
    public List<UserResponse> getUser() {
        return userMapper.toUserResponseList(userRepo.findAll());
    }

    // Lấy profile của đúng user đang đăng nhập dựa trên userId lấy từ token.
    public UserProfileResponse getProfile(String userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return buildUserProfileResponse(user);
    }

    // Cập nhật profile gồm fullname, school, avatar và mật khẩu nếu người dùng có yêu cầu đổi mật khẩu.
    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        validateFullname(request.getFullname());

        String oldAvatarUrl = user.getAvatarUrl();
        String newAvatarUrl = CloudinaryStorageService.saveAvatar(userId, request.getAvatar());

        user.setFullname(request.getFullname().trim());
        user.setSchool(normalizeOptionalText(request.getSchool()));

        if (newAvatarUrl != null) {
            user.setAvatarUrl(newAvatarUrl);
        }

        updatePasswordIfRequested(user, request);

        try {
            User savedUser = userRepo.save(user);

            if (newAvatarUrl != null) {
                CloudinaryStorageService.deleteAvatar(oldAvatarUrl);
            }

            return buildUserProfileResponse(savedUser);
        } catch (RuntimeException exception) {
            CloudinaryStorageService.deleteAvatar(newAvatarUrl);
            throw exception;
        }
    }

    // Build profile response và bổ sung các thống kê phụ như số tài liệu và số ngày đăng nhập liên tiếp.
    private UserProfileResponse buildUserProfileResponse(User user) {
        UserProfileResponse response = userMapper.toUserProfileResponse(user);
        response.setDocumentCount(documentRepo.countActiveByOwner(user));
        response.setConsecutiveLoginDays(countConsecutiveLoginDays(user));

        return response;
    }

    // Đếm số ngày đăng nhập liên tiếp của user dựa trên lịch sử login đã được lưu theo từng ngày.
    private long countConsecutiveLoginDays(User user) {
        List<LocalDate> loginDates = userLoginHistoryRepo.findLoginDatesByUserOrderByLoginDateDesc(user);
        if (loginDates.isEmpty()) {
            return 0;
        }

        long consecutiveDays = 0;
        LocalDate expectedDate = loginDates.get(0);

        for (LocalDate loginDate : loginDates) {
            if (!loginDate.equals(expectedDate)) {
                break;
            }

            consecutiveDays++;
            expectedDate = expectedDate.minusDays(1);
        }

        return consecutiveDays;
    }

    // Kiểm tra fullname không được rỗng và không vượt quá độ dài cho phép.
    private void validateFullname(String fullname) {
        if (fullname == null || fullname.isBlank() || fullname.length() > 100) {
            throw new AppException(ErrorCode.PROFILE_FULLNAME_INVALID);
        }
    }

    // Chuẩn hóa field optional: null hoặc chuỗi rỗng được lưu thành null, chuỗi hợp lệ được trim.
    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    // Chỉ đổi mật khẩu khi request có ít nhất một field password; nếu có thì validate đủ mật khẩu cũ và mới.
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

    // Xác định request có đang yêu cầu đổi mật khẩu hay chỉ cập nhật thông tin profile thông thường.
    private boolean isPasswordChangeRequested(UpdateProfileRequest request) {
        return hasText(request.getOldPassword())
                || hasText(request.getNewPassword())
                || hasText(request.getConfirmPassword());
    }

    // Kiểm tra mật khẩu mới đạt yêu cầu tối thiểu: ít nhất 8 ký tự và có ký tự đặc biệt.
    private void validateNewPassword(String newPassword) {
        if (!hasText(newPassword)
                || newPassword.length() < 8
                || !newPassword.matches(".*[^a-zA-Z0-9].*")) {
            throw new AppException(ErrorCode.PROFILE_PASSWORD_INVALID);
        }
    }

    // Kiểm tra chuỗi có nội dung thật sau khi bỏ khoảng trắng hay không.
    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
