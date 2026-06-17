package com.pctb.webapp.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProfileRequest {
    // Họ tên bắt buộc nhập khi cập nhật profile.
    @Size(max = 100, message = "PROFILE_FULLNAME_INVALID")
    String fullname;

    // Trường học do user tự nhập, có thể bỏ trống.
    @Size(max = 100, message = "PROFILE_SCHOOL_INVALID")
    String school;

    // File avatar upload từ Swagger/FE, chỉ validate trong LocalStorageService.
    @Schema(type = "string", format = "binary")
    MultipartFile avatar;

    // Mật khẩu cũ, chỉ bắt buộc khi user muốn đổi password.
    String oldPassword;

    // Mật khẩu mới, chỉ bắt buộc khi user muốn đổi password.
    String newPassword;

    // Xác nhận mật khẩu mới, phải khớp với newPassword khi đổi password.
    String confirmPassword;
}
