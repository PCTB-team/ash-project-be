package com.pctb.webapp.exception;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@AllArgsConstructor
public enum ErrorCode {
    // Dùng khi email trong request bị trống, sai format, hoặc vượt quá giới hạn độ dài.
    EMAIL_INVALID(1001, "Email is invalid", HttpStatus.BAD_REQUEST),

    // Dùng khi đăng ký hoặc xác thực đăng ký nhưng email đã tồn tại trong hệ thống.
    EMAIL_ALREADY_EXISTS(1002, "Email already exists", HttpStatus.CONFLICT),

    // Dùng khi đăng ký hoặc xác thực đăng ký nhưng username đã tồn tại trong hệ thống.
    USERNAME_ALREADY_EXISTS(1003, "Username already exists", HttpStatus.CONFLICT),

    // Dùng khi username đăng ký bị trống, không nằm trong 3-20 ký tự, hoặc chứa ký tự đặc biệt.
    USERNAME_INVALID(1004, "Username must be between 3 and 20 characters and contain no special characters", HttpStatus.BAD_REQUEST),

    // Dùng khi password đăng ký/reset bị trống, dưới 8 ký tự, hoặc thiếu ký tự đặc biệt.
    PASSWORD_INVALID(1005, "Password must be at least 8 characters and contain at least 1 special character", HttpStatus.BAD_REQUEST),

    // Dùng khi confirmPassword của đăng ký không khớp với password.
    CONFIRM_PASSWORD_NOT_MATCH(1006, "Confirm password does not match", HttpStatus.BAD_REQUEST),

    // Dùng khi không còn dữ liệu đăng ký tạm trong Redis để gửi lại hoặc xác thực OTP đăng ký.
    REGISTER_SESSION_EXPIRED(1007, "Registration session expired. Please register again", HttpStatus.BAD_REQUEST),

    // Dùng khi email đã gửi OTP vượt quá số lần cho phép trong ngày.
    OTP_SEND_LIMIT_EXCEEDED(1008, "OTP send limit exceeded", HttpStatus.TOO_MANY_REQUESTS),

    // Dùng khi người dùng yêu cầu gửi lại OTP trước khi hết thời gian cooldown.
    OTP_RESEND_TOO_SOON(1009, "Please wait 60 seconds before requesting a new OTP", HttpStatus.TOO_MANY_REQUESTS),

    // Dùng khi OTP đăng ký bị trống, sai format 6 số, hoặc không khớp OTP đã lưu.
    OTP_INVALID(1010, "OTP is invalid", HttpStatus.BAD_REQUEST),

    // Dùng khi OTP đăng ký đã hết hạn hoặc không còn trong Redis.
    OTP_EXPIRED(1011, "OTP has expired", HttpStatus.BAD_REQUEST),

    // Dùng khi tài khoản đã verified nhưng vẫn gọi luồng resend OTP đăng ký.
    ACCOUNT_ALREADY_VERIFIED(1012, "Account already verified", HttpStatus.CONFLICT),

    // Dùng khi identifier login bị trống; identifier có thể là email hoặc username.
    IDENTIFIER_REQUIRED(1013, "Email or username is required", HttpStatus.BAD_REQUEST),

    // Dùng khi password login bị trống.
    PASSWORD_REQUIRED(1014, "Password is required", HttpStatus.BAD_REQUEST),

    // Dùng khi tài khoản tồn tại nhưng chưa xác thực email nên không được login/refresh.
    ACCOUNT_NOT_VERIFIED(1015, "Account is not verified", HttpStatus.FORBIDDEN),

    // Dùng khi identifier hoặc password login không đúng.
    USERNAME_OR_PASSWORD_INCORRECT(1016, "Username or password is incorrect", HttpStatus.UNAUTHORIZED),

    // Dùng khi số lần login sai vượt quá giới hạn trong cửa sổ thời gian cấu hình.
    LOGIN_ATTEMPTS_EXCEEDED(1017, "Login attempts exceeded. Please try again later", HttpStatus.TOO_MANY_REQUESTS),

    // Dùng khi refreshToken trong request bị trống.
    REFRESH_TOKEN_REQUIRED(1018, "Refresh token is required", HttpStatus.BAD_REQUEST),

    // Dùng khi refresh token sai chữ ký, sai loại token, không khớp Redis, hoặc không cùng user với access token.
    REFRESH_TOKEN_INVALID(1019, "Refresh token is invalid", HttpStatus.UNAUTHORIZED),

    // Dùng khi access token hoặc refresh token đã quá hạn.
    TOKEN_EXPIRED(1020, "Token has expired", HttpStatus.UNAUTHORIZED),

    // Dùng khi logout nhưng refresh token của user không còn trong Redis.
    ACCOUNT_ALREADY_LOGGED_OUT(1021, "Account already logged out", HttpStatus.CONFLICT),

    // Dùng khi access token bị thiếu, trống, sai chữ ký, sai loại token, hoặc không hợp lệ.
    UNAUTHENTICATED(1022, "Unauthenticated", HttpStatus.UNAUTHORIZED),

    // Dùng khi forgot password gửi OTP/reset nhưng email không tồn tại trong hệ thống.
    EMAIL_NOT_EXISTED(1023, "Email does not exist in the system", HttpStatus.NOT_FOUND),

    // Dùng khi OTP forgot password bị trống, sai format 6 số, hoặc không khớp OTP đã lưu.
    FORGOT_PASSWORD_OTP_INVALID(1024, "Invalid OTP verification code", HttpStatus.BAD_REQUEST),

    // Dùng khi OTP forgot password đã hết hạn hoặc không còn trong Redis.
    FORGOT_PASSWORD_OTP_EXPIRED(1025, "OTP code has expired or does not exist", HttpStatus.BAD_REQUEST),

    // Dùng khi reset token bị trống, hết hạn, hoặc không tồn tại trong Redis.
    RESET_TOKEN_INVALID(1026, "Session has expired or password reset token is invalid", HttpStatus.UNAUTHORIZED),

    // Dùng khi confirmPassword của reset password không khớp với newPassword.
    RESET_PASSWORD_MISMATCH(1027, "Confirm password does not match new password", HttpStatus.BAD_REQUEST),

    // Dùng khi thao tác lưu password mới thất bại trong luồng reset password.
    RESET_PASSWORD_FAILED(1028, "Failed to update new password", HttpStatus.INTERNAL_SERVER_ERROR),

    // Dùng khi token Google login bị trống.
    GOOGLE_TOKEN_REQUIRED(1029, "Google token is required", HttpStatus.BAD_REQUEST),

    // Dùng khi token Google login không verify được, không đúng audience, hoặc email chưa verified.
    GOOGLE_TOKEN_INVALID(1030, "Google token is invalid", HttpStatus.UNAUTHORIZED),

    // Dùng khi role bắt buộc, ví dụ USER/ADMIN, không tồn tại trong database.
    ROLE_NOT_FOUND(1031, "Role not found", HttpStatus.INTERNAL_SERVER_ERROR),

    // Dùng khi fullname đăng ký bị trống hoặc vượt quá giới hạn độ dài.
    FULLNAME_INVALID(1032, "Full name is invalid", HttpStatus.BAD_REQUEST),

    // Dùng khi userId lấy từ token hợp lệ nhưng không tìm thấy user trong database.
    USER_NOT_FOUND(1033, "User not found", HttpStatus.NOT_FOUND),

    // Dùng khi key của Redis API bị trống.
    KEY_REQUIRED(1101, "Key is required", HttpStatus.BAD_REQUEST),

    // Dùng khi value của Redis set/set-with-ttl bị trống.
    VALUE_REQUIRED(1102, "Value is required", HttpStatus.BAD_REQUEST),

    // Dùng khi TTL của Redis API bị trống hoặc nhỏ hơn 1 giây.
    TTL_INVALID(1103, "TTL must be greater than 0", HttpStatus.BAD_REQUEST),

    // Dùng khi request body bị thiếu hoặc không đọc được JSON.
    REQUEST_BODY_INVALID(1201, "Request body is invalid", HttpStatus.BAD_REQUEST),

    // Dùng khi query/path parameter bị thiếu, sai kiểu dữ liệu, hoặc không map được về ErrorCode cụ thể hơn.
    REQUEST_PARAMETER_INVALID(1202, "Request parameter is invalid", HttpStatus.BAD_REQUEST),

    // Tiêu đề tài liệu bị bỏ trống hoặc chỉ chứa khoảng trắng
    DOCUMENT_TITLE_REQUIRED(1301, "Document title is required", HttpStatus.BAD_REQUEST),

    // Người dùng không gửi file trong request upload
    FILE_REQUIRED(1302, "File is required", HttpStatus.BAD_REQUEST),

    // Kích thước file vượt quá giới hạn cho phép của hệ thống
    FILE_TOO_LARGE(1303, "File exceeds maximum allowed size", HttpStatus.BAD_REQUEST),

    // Định dạng file không nằm trong danh sách được hỗ trợ
// (pdf, docx, png, jpg, jpeg, txt, mp3, mp4, ppt, pptx)
    FILE_TYPE_NOT_SUPPORTED(1304, "File type is not supported", HttpStatus.BAD_REQUEST),

    // Phần mở rộng file và MIME Type thực tế không khớp nhau
// Ví dụ: đổi tên virus.exe thành document.pdf
    INVALID_MIME_TYPE(1305, "Invalid mime type or fake file extension", HttpStatus.BAD_REQUEST),

    // Dung lượng lưu trữ còn lại của người dùng không đủ để upload file
    STORAGE_NOT_ENOUGH(1306, "Cloud storage is not enough. Please upgrade your storage", HttpStatus.FORBIDDEN),

    // Đã tồn tại file cùng tên và người dùng không chọn chế độ ghi đè (Replace)
    FILE_ALREADY_EXISTS(1307, "File already exists", HttpStatus.CONFLICT),

    // Upload thất bại do lỗi hệ thống
// Ví dụ: lỗi Cloudinary, AWS S3, Database hoặc kết nối mạng
    UPLOAD_FAILED(1308, "Upload failed", HttpStatus.INTERNAL_SERVER_ERROR),

    // Không tìm thấy phiên upload
// Có thể phiên đã hết hạn, bị xóa hoặc id không hợp lệ
    UPLOAD_SESSION_NOT_FOUND(1309, "Upload session not found", HttpStatus.NOT_FOUND),

    // Không thể tạm dừng upload ở trạng thái hiện tại
// Ví dụ: upload đã hoàn thành, đã hủy hoặc đang tạm dừng
    UPLOAD_CANNOT_BE_PAUSED(1310, "Upload cannot be paused", HttpStatus.BAD_REQUEST),

    // Không thể tiếp tục upload ở trạng thái hiện tại
// Ví dụ: upload đã hoàn thành, đã hủy hoặc chưa từng bị tạm dừng
    UPLOAD_CANNOT_BE_RESUMED(1311, "Upload cannot be resumed", HttpStatus.BAD_REQUEST),

    // Không thể hủy upload ở trạng thái hiện tại
// Ví dụ: upload đã hoàn thành hoặc đã bị hủy trước đó
    UPLOAD_CANNOT_BE_CANCELED(1312, "Upload cannot be canceled", HttpStatus.BAD_REQUEST),

    DOCUMENT_NOT_FOUND(1313, "Document not found", HttpStatus.NOT_FOUND),

    DOCUMENT_ACCESS_DENIED(1314, "Document access denied", HttpStatus.FORBIDDEN),

    DOCUMENT_FILE_NAME_REQUIRED(1315, "Document file name is required", HttpStatus.BAD_REQUEST),

    DOCUMENT_EXTENSION_CANNOT_CHANGE(1316, "Document file extension cannot be changed", HttpStatus.BAD_REQUEST),

    DOCUMENT_NOT_IN_TRASH(1317, "Document is not in trash", HttpStatus.BAD_REQUEST);


    private final int code;

    private final String message;
    private final HttpStatusCode statusCode;

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
