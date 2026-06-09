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
    USER_NOT_FOUND(1204, "User not found", HttpStatus.NOT_FOUND),

    // Dùng khi key của Redis API bị trống.
    KEY_REQUIRED(1101, "Key is required", HttpStatus.BAD_REQUEST),

    // Dùng khi value của Redis set/set-with-ttl bị trống.
    VALUE_REQUIRED(1102, "Value is required", HttpStatus.BAD_REQUEST),

    // Dùng khi TTL của Redis API bị trống hoặc nhỏ hơn 1 giây.
    TTL_INVALID(1103, "TTL must be greater than 0", HttpStatus.BAD_REQUEST),

    // Dùng khi hệ thống cache Redis gặp sự cố kết nối, không thể đọc hoặc ghi dữ liệu tạm thời.
    REDIS_CONNECTION_FAILED(1104, "Cache service connection failed", HttpStatus.INTERNAL_SERVER_ERROR),

    // Dùng khi file tải lên (như ảnh đại diện nhóm, tài liệu học tập) bị trống.
    FILE_REQUIRED(1105, "Uploaded file cannot be empty", HttpStatus.BAD_REQUEST),

    // Dùng khi dung lượng file vượt quá giới hạn hệ thống cho phép (ví dụ: lớn hơn 5MB).
    FILE_SIZE_EXCEEDED(1106, "File size exceeds the maximum permitted limit", HttpStatus.CONTENT_TOO_LARGE),

    // Dùng khi định dạng file không hợp lệ (ví dụ: chỉ nhận .png/.jpg nhưng lại tải lên file khác).
    INVALID_FILE_FORMAT(1107, "Unsupported file format. Only specific formats are allowed", HttpStatus.BAD_REQUEST),

    // Dùng khi thành viên thường cố tình thực hiện quyền của OWNER hoặc ADMIN (như kích người, xóa nhóm).
    UNAUTHORIZED_GROUP_ACTION(1213, "You do not have permission to perform this action in the group", HttpStatus.FORBIDDEN),

    // Dùng khi người dùng cần tương tác (ví dụ: kích thành viên) không nằm trong nhóm học tập này.
    MEMBER_NOT_FOUND_IN_GROUP(1214, "The specified user is not a member of this group", HttpStatus.NOT_FOUND),

    // Dùng khi chủ nhóm (OWNER) cố tình rời nhóm khi chưa nhường quyền hoặc giải tán nhóm.
    OWNER_CANNOT_LEAVE_GROUP(1215, "Group owner cannot leave the group without assigning a new owner", HttpStatus.BAD_REQUEST),

    // Dùng khi mã mời hoặc lời mời tham gia vào nhóm học tập đã bị hủy hoặc hết hạn sử dụng.
    GROUP_INVITATION_EXPIRED(1216, "The group invitation code or link has expired", HttpStatus.BAD_REQUEST),

    // Dùng khi không tìm thấy nhóm dựa trên Join Code người dùng cung cấp.
    GROUP_NOT_FOUND(1217, "Group not found with the provided join code", HttpStatus.NOT_FOUND),

    // Dùng khi người dùng thực chất đã là thành viên hoặc chủ nhóm từ trước rồi.
    USER_ALREADY_IN_GROUP(1218, "You are already a member of this group", HttpStatus.BAD_REQUEST),

    // Dùng khi nhóm đang để chế độ riêng tư, không cho phép tự do tham gia qua mã code công khai.
    GROUP_IS_PRIVATE(1219, "This group is private and requires an invitation to join", HttpStatus.FORBIDDEN),

    GROUP_DOCUMENT_UPLOAD_NOT_ALLOWED(1220, "Member is not allowed to upload documents in this group", HttpStatus.FORBIDDEN),

    // Dùng khi request body bị thiếu hoặc không đọc được JSON.
    REQUEST_BODY_INVALID(1201, "Request body is invalid", HttpStatus.BAD_REQUEST),

    // Dùng khi query/path parameter bị thiếu, sai kiểu dữ liệu, hoặc không map được về ErrorCode cụ thể hơn.
    REQUEST_PARAMETER_INVALID(1202, "Request parameter is invalid", HttpStatus.BAD_REQUEST),

    // Tiêu đề tài liệu bị bỏ trống hoặc chỉ chứa khoảng trắng
    DOCUMENT_TITLE_REQUIRED(1301, "Document title is required", HttpStatus.BAD_REQUEST),

    // Người dùng không gửi file trong request upload
    FILE_REQUIRED_UPLOAD(1302, "File is required", HttpStatus.BAD_REQUEST),

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

    DOCUMENT_NOT_IN_TRASH(1317, "Document is not in trash", HttpStatus.BAD_REQUEST),

    // Nhóm lỗi update profile dùng mã 12xx theo quy định của team.
    PROFILE_FULLNAME_INVALID(1203, "Full name is invalid", HttpStatus.BAD_REQUEST),

    OLD_PASSWORD_INCORRECT(1205, "Old password is incorrect", HttpStatus.BAD_REQUEST),

    NEW_PASSWORD_SAME_AS_OLD(1206, "New password must be different from old password", HttpStatus.BAD_REQUEST),

    PROFILE_PASSWORD_INVALID(1207, "Password must be at least 8 characters and contain at least 1 special character", HttpStatus.BAD_REQUEST),

    PROFILE_CONFIRM_PASSWORD_NOT_MATCH(1208, "Confirm password does not match new password", HttpStatus.BAD_REQUEST),

    AVATAR_TYPE_INVALID(1209, "Avatar type is invalid. Only png, jpg, jpeg are supported", HttpStatus.BAD_REQUEST),

    AVATAR_SIZE_EXCEEDED(1210, "Avatar size must not exceed 5MB", HttpStatus.BAD_REQUEST),

    AVATAR_UPLOAD_FAILED(1211, "Avatar upload failed", HttpStatus.INTERNAL_SERVER_ERROR),

    PROFILE_SCHOOL_INVALID(1212, "School is invalid", HttpStatus.BAD_REQUEST);

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
