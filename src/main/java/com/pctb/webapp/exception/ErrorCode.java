package com.pctb.webapp.exception;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@AllArgsConstructor
public enum ErrorCode {
    // Dùng khi email trong request bị trống, sai format, hoặc vượt quá giới hạn độ dài.
    EMAIL_INVALID(1001, "Email không hợp lệ", HttpStatus.BAD_REQUEST),

    // Dùng khi đăng ký hoặc xác thực đăng ký nhưng email đã tồn tại trong hệ thống.
    EMAIL_ALREADY_EXISTS(1002, "Email đã tồn tại", HttpStatus.CONFLICT),

    // Dùng khi đăng ký hoặc xác thực đăng ký nhưng username đã tồn tại trong hệ thống.
    USERNAME_ALREADY_EXISTS(1003, "Tên đăng nhập đã tồn tại", HttpStatus.CONFLICT),

    // Dùng khi username đăng ký bị trống, không nằm trong 3-20 ký tự, hoặc chứa ký tự đặc biệt.
    USERNAME_INVALID(1004, "Tên đăng nhập phải từ 3 đến 20 ký tự và không chứa ký tự đặc biệt", HttpStatus.BAD_REQUEST),

    // Dùng khi password đăng ký/reset bị trống, dưới 8 ký tự, hoặc thiếu ký tự đặc biệt.
    PASSWORD_INVALID(1005, "Mật khẩu phải có ít nhất 8 ký tự và chứa ít nhất 1 ký tự đặc biệt", HttpStatus.BAD_REQUEST),

    // Dùng khi confirmPassword của đăng ký không khớp với password.
    CONFIRM_PASSWORD_NOT_MATCH(1006, "Xác nhận mật khẩu không khớp", HttpStatus.BAD_REQUEST),

    // Dùng khi không còn dữ liệu đăng ký tạm trong Redis để gửi lại hoặc xác thực OTP đăng ký.
    REGISTER_SESSION_EXPIRED(1007, "Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại", HttpStatus.BAD_REQUEST),

    // Dùng khi email đã gửi OTP vượt quá số lần cho phép trong ngày.
    OTP_SEND_LIMIT_EXCEEDED(1008, "Đã vượt quá số lần gửi OTP cho phép", HttpStatus.TOO_MANY_REQUESTS),

    // Dùng khi người dùng yêu cầu gửi lại OTP trước khi hết thời gian cooldown.
    OTP_RESEND_TOO_SOON(1009, "Vui lòng chờ 60 giây trước khi yêu cầu OTP mới", HttpStatus.TOO_MANY_REQUESTS),

    // Dùng khi OTP đăng ký bị trống, sai format 6 số, hoặc không khớp OTP đã lưu.
    OTP_INVALID(1010, "OTP không hợp lệ", HttpStatus.BAD_REQUEST),

    // Dùng khi OTP đăng ký đã hết hạn hoặc không còn trong Redis.
    OTP_EXPIRED(1011, "OTP đã hết hạn", HttpStatus.BAD_REQUEST),

    // Dùng khi tài khoản đã verified nhưng vẫn gọi luồng resend OTP đăng ký.
    ACCOUNT_ALREADY_VERIFIED(1012, "Tài khoản đã được xác thực", HttpStatus.CONFLICT),

    // Dùng khi identifier login bị trống; identifier có thể là email hoặc username.
    IDENTIFIER_REQUIRED(1013, "Yêu cầu email hoặc tên đăng nhập", HttpStatus.BAD_REQUEST),

    // Dùng khi password login bị trống.
    PASSWORD_REQUIRED(1014, "Yêu cầu mật khẩu", HttpStatus.BAD_REQUEST),

    // Dùng khi tài khoản tồn tại nhưng chưa xác thực email nên không được login/refresh.
    ACCOUNT_NOT_VERIFIED(1015, "Tài khoản chưa được xác thực", HttpStatus.FORBIDDEN),

    // Dùng khi identifier hoặc password login không đúng.
    USERNAME_OR_PASSWORD_INCORRECT(1016, "Tên đăng nhập hoặc mật khẩu không chính xác", HttpStatus.UNAUTHORIZED),

    // Dùng khi số lần login sai vượt quá giới hạn trong cửa sổ thời gian cấu hình.
    LOGIN_ATTEMPTS_EXCEEDED(1017, "Đã vượt quá số lần đăng nhập. Vui lòng thử lại sau", HttpStatus.TOO_MANY_REQUESTS),

    // Dùng khi refreshToken trong request bị trống.
    REFRESH_TOKEN_REQUIRED(1018, "Yêu cầu refresh token", HttpStatus.BAD_REQUEST),

    // Dùng khi refresh token sai chữ ký, sai loại token, không khớp Redis, hoặc không cùng user với access token.
    REFRESH_TOKEN_INVALID(1019, "Refresh token không hợp lệ", HttpStatus.UNAUTHORIZED),

    // Dùng khi access token hoặc refresh token đã quá hạn.
    TOKEN_EXPIRED(1020, "Token đã hết hạn", HttpStatus.UNAUTHORIZED),

    // Dùng khi logout nhưng refresh token của user không còn trong Redis.
    ACCOUNT_ALREADY_LOGGED_OUT(1021, "Tài khoản đã đăng xuất", HttpStatus.CONFLICT),

    // Dùng khi access token bị thiếu, trống, sai chữ ký, sai loại token, hoặc không hợp lệ.
    UNAUTHENTICATED(1022, "Chưa xác thực", HttpStatus.UNAUTHORIZED),

    // Dùng khi forgot password gửi OTP/reset nhưng email không tồn tại trong hệ thống.
    EMAIL_NOT_EXISTED(1023, "Email không tồn tại trong hệ thống", HttpStatus.NOT_FOUND),

    // Dùng khi OTP forgot password bị trống, sai format 6 số, hoặc không khớp OTP đã lưu.
    FORGOT_PASSWORD_OTP_INVALID(1024, "Mã OTP xác thực không hợp lệ", HttpStatus.BAD_REQUEST),

    // Dùng khi OTP forgot password đã hết hạn hoặc không còn trong Redis.
    FORGOT_PASSWORD_OTP_EXPIRED(1025, "Mã OTP đã hết hạn hoặc không tồn tại", HttpStatus.BAD_REQUEST),

    // Dùng khi reset token bị trống, hết hạn, hoặc không tồn tại trong Redis.
    RESET_TOKEN_INVALID(1026, "Phiên đã hết hạn hoặc token đặt lại mật khẩu không hợp lệ", HttpStatus.UNAUTHORIZED),

    // Dùng khi confirmPassword của reset password không khớp với newPassword.
    RESET_PASSWORD_MISMATCH(1027, "Xác nhận mật khẩu không khớp với mật khẩu mới", HttpStatus.BAD_REQUEST),

    // Dùng khi thao tác lưu password mới thất bại trong luồng reset password.
    RESET_PASSWORD_FAILED(1028, "Cập nhật mật khẩu mới thất bại", HttpStatus.INTERNAL_SERVER_ERROR),

    // Dùng khi token Google login bị trống.
    GOOGLE_TOKEN_REQUIRED(1029, "Yêu cầu Google token", HttpStatus.BAD_REQUEST),

    // Dùng khi token Google login không verify được, không đúng audience, hoặc email chưa verified.
    GOOGLE_TOKEN_INVALID(1030, "Google token không hợp lệ", HttpStatus.UNAUTHORIZED),

    // Dùng khi role bắt buộc, ví dụ USER/ADMIN, không tồn tại trong database.
    ROLE_NOT_FOUND(1031, "Không tìm thấy vai trò", HttpStatus.INTERNAL_SERVER_ERROR),

    // Dùng khi fullname đăng ký bị trống hoặc vượt quá giới hạn độ dài.
    FULLNAME_INVALID(1032, "Họ và tên không hợp lệ", HttpStatus.BAD_REQUEST),

    // Dùng khi userId lấy từ token hợp lệ nhưng không tìm thấy user trong database.
    USER_NOT_FOUND(1204, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),

    // Dùng khi key của Redis API bị trống.
    KEY_REQUIRED(1101, "Yêu cầu khóa", HttpStatus.BAD_REQUEST),

    // Dùng khi value của Redis set/set-with-ttl bị trống.
    VALUE_REQUIRED(1102, "Yêu cầu giá trị", HttpStatus.BAD_REQUEST),

    // Dùng khi TTL của Redis API bị trống hoặc nhỏ hơn 1 giây.
    TTL_INVALID(1103, "TTL phải lớn hơn 0", HttpStatus.BAD_REQUEST),

    // Dùng khi hệ thống cache Redis gặp sự cố kết nối, không thể đọc hoặc ghi dữ liệu tạm thời.
    REDIS_CONNECTION_FAILED(1104, "Kết nối dịch vụ cache thất bại", HttpStatus.INTERNAL_SERVER_ERROR),

    // Dùng khi file tải lên bị trống.
    FILE_REQUIRED(1105, "Tệp tải lên không được để trống", HttpStatus.BAD_REQUEST),

    // Dùng khi dung lượng file vượt quá giới hạn hệ thống cho phép (ví dụ: lớn hơn 5MB).
    FILE_SIZE_EXCEEDED(1106, "Kích thước tệp vượt quá giới hạn cho phép", HttpStatus.CONTENT_TOO_LARGE),

    // Dùng khi định dạng file không hợp lệ (ví dụ: chỉ nhận .png/.jpg nhưng lại tải lên file khác).
    INVALID_FILE_FORMAT(1107, "Định dạng tệp không được hỗ trợ", HttpStatus.BAD_REQUEST),

    // Dùng khi request body bị thiếu hoặc không đọc được JSON.
    REQUEST_BODY_INVALID(1201, "Dữ liệu request không hợp lệ", HttpStatus.BAD_REQUEST),

    // Dùng khi query/path parameter bị thiếu, sai kiểu dữ liệu, hoặc không map được về ErrorCode cụ thể hơn.
    REQUEST_PARAMETER_INVALID(1202, "Tham số request không hợp lệ", HttpStatus.BAD_REQUEST),

    // Tiêu đề tài liệu bị bỏ trống hoặc chỉ chứa khoảng trắng
    DOCUMENT_TITLE_REQUIRED(1301, "Yêu cầu tiêu đề tài liệu", HttpStatus.BAD_REQUEST),

    // Người dùng không gửi file trong request upload
    FILE_REQUIRED_UPLOAD(1302, "Yêu cầu tệp", HttpStatus.BAD_REQUEST),

    // Kích thước file vượt quá giới hạn cho phép của hệ thống
    FILE_TOO_LARGE(1303, "Tệp vượt quá kích thước cho phép", HttpStatus.BAD_REQUEST),

    // Định dạng file không nằm trong danh sách được hỗ trợ
// (pdf, docx, png, jpg, jpeg, txt, mp3, mp4, ppt, pptx)
    FILE_TYPE_NOT_SUPPORTED(1304, "Loại tệp không được hỗ trợ", HttpStatus.BAD_REQUEST),

    // Phần mở rộng file và MIME Type thực tế không khớp nhau
// Ví dụ: đổi tên virus.exe thành document.pdf
    INVALID_MIME_TYPE(1305, "Mime type không hợp lệ hoặc phần mở rộng tệp giả mạo", HttpStatus.BAD_REQUEST),

    // Dung lượng lưu trữ còn lại của người dùng không đủ để upload file
    STORAGE_NOT_ENOUGH(1306, "Dung lượng lưu trữ không đủ. Vui lòng nâng cấp", HttpStatus.FORBIDDEN),

    // Đã tồn tại file cùng tên và người dùng không chọn chế độ ghi đè (Replace)
    FILE_ALREADY_EXISTS(1307, "Tệp đã tồn tại", HttpStatus.CONFLICT),

    // Upload thất bại do lỗi hệ thống
// Ví dụ: lỗi Cloudinary, AWS S3, Database hoặc kết nối mạng
    UPLOAD_FAILED(1308, "Tải lên thất bại", HttpStatus.INTERNAL_SERVER_ERROR),

    // Không tìm thấy phiên upload
// Có thể phiên đã hết hạn, bị xóa hoặc id không hợp lệ
    UPLOAD_SESSION_NOT_FOUND(1309, "Không tìm thấy phiên tải lên", HttpStatus.NOT_FOUND),

    // Không thể tạm dừng upload ở trạng thái hiện tại
// Ví dụ: upload đã hoàn thành, đã hủy hoặc đang tạm dừng
    UPLOAD_CANNOT_BE_PAUSED(1310, "Không thể tạm dừng tải lên", HttpStatus.BAD_REQUEST),

    // Không thể tiếp tục upload ở trạng thái hiện tại
// Ví dụ: upload đã hoàn thành, đã hủy hoặc chưa từng bị tạm dừng
    UPLOAD_CANNOT_BE_RESUMED(1311, "Không thể tiếp tục tải lên", HttpStatus.BAD_REQUEST),

    // Không thể hủy upload ở trạng thái hiện tại
// Ví dụ: upload đã hoàn thành hoặc đã bị hủy trước đó
    UPLOAD_CANNOT_BE_CANCELED(1312, "Không thể hủy tải lên", HttpStatus.BAD_REQUEST),

    DOCUMENT_NOT_FOUND(1313, "Không tìm thấy tài liệu", HttpStatus.NOT_FOUND),

    DOCUMENT_ACCESS_DENIED(1314, "Không có quyền truy cập tài liệu", HttpStatus.FORBIDDEN),

    DOCUMENT_FILE_NAME_REQUIRED(1315, "Yêu cầu tên tệp tài liệu", HttpStatus.BAD_REQUEST),

    DOCUMENT_EXTENSION_CANNOT_CHANGE(1316, "Không thể thay đổi phần mở rộng tệp", HttpStatus.BAD_REQUEST),

    DOCUMENT_NOT_IN_TRASH(1317, "Tài liệu không nằm trong thùng rác", HttpStatus.BAD_REQUEST),

    // Các lỗi update profile dùng mã 12xx theo quy định của team.
    PROFILE_FULLNAME_INVALID(1203, "Họ và tên không hợp lệ", HttpStatus.BAD_REQUEST),

    OLD_PASSWORD_INCORRECT(1205, "Mật khẩu cũ không chính xác", HttpStatus.BAD_REQUEST),

    NEW_PASSWORD_SAME_AS_OLD(1206, "Mật khẩu mới phải khác mật khẩu cũ", HttpStatus.BAD_REQUEST),

    PROFILE_PASSWORD_INVALID(1207, "Mật khẩu phải có ít nhất 8 ký tự và chứa ít nhất 1 ký tự đặc biệt", HttpStatus.BAD_REQUEST),

    PROFILE_CONFIRM_PASSWORD_NOT_MATCH(1208, "Xác nhận mật khẩu không khớp với mật khẩu mới", HttpStatus.BAD_REQUEST),

    AVATAR_TYPE_INVALID(1209, "Loại ảnh đại diện không hợp lệ. Chỉ hỗ trợ png, jpg, jpeg", HttpStatus.BAD_REQUEST),

    AVATAR_SIZE_EXCEEDED(1210, "Ảnh đại diện không được vượt quá 5MB", HttpStatus.BAD_REQUEST),

    AVATAR_UPLOAD_FAILED(1211, "Tải ảnh đại diện thất bại", HttpStatus.INTERNAL_SERVER_ERROR),

    PROFILE_SCHOOL_INVALID(1212, "Trường học không hợp lệ", HttpStatus.BAD_REQUEST),

    // Group errors use 12xx according to team convention.
    GROUP_NOT_FOUND(1213, "Không tìm thấy nhóm", HttpStatus.NOT_FOUND),

    GROUP_INVITE_DISABLED(1214, "Liên kết mời nhóm đã bị vô hiệu hóa", HttpStatus.BAD_REQUEST),

    GROUP_PASSWORD_INCORRECT(1215, "Mật khẩu nhóm không chính xác", HttpStatus.BAD_REQUEST),

    USER_ALREADY_IN_GROUP(1216, "Người dùng đã tham gia nhóm này", HttpStatus.CONFLICT),

    GROUP_JOIN_REQUEST_PENDING(1217, "Yêu cầu tham gia đang chờ xử lý", HttpStatus.CONFLICT),

    GROUP_MEMBER_NOT_FOUND(1218, "Không tìm thấy thành viên nhóm", HttpStatus.NOT_FOUND),

    GROUP_ACCESS_DENIED(1219, "Bạn không có quyền trong nhóm này", HttpStatus.FORBIDDEN),

    GROUP_UPLOAD_NOT_ALLOWED(1220, "Bạn không được phép tải tệp lên nhóm này", HttpStatus.FORBIDDEN),

    GROUP_MEMBER_NOT_APPROVED(1221, "Thành viên chưa được phê duyệt", HttpStatus.FORBIDDEN),

    GROUP_MEMBER_BANNED(1222, "Bạn đã bị cấm khỏi nhóm", HttpStatus.FORBIDDEN),

    GROUP_MEMBER_NOT_PENDING(1223, "Yêu cầu tham gia không ở trạng thái chờ", HttpStatus.BAD_REQUEST),

    GROUP_MEMBER_ALREADY_REJECTED(1224, "Yêu cầu tham gia đã bị từ chối", HttpStatus.CONFLICT),

    GROUP_INVITE_TOKEN_GENERATION_FAILED(1225, "Tạo token mời nhóm thất bại", HttpStatus.INTERNAL_SERVER_ERROR),

    GROUP_LEADER_CANNOT_BE_KICKED(1226, "Không thể đuổi trưởng nhóm", HttpStatus.BAD_REQUEST),

    GROUP_MEMBER_NOT_APPROVED_TO_KICK(1227, "Chỉ thành viên đã được duyệt mới có thể bị đuổi", HttpStatus.BAD_REQUEST),

    GROUP_FILE_NOT_FOUND(1228, "Không tìm thấy tệp của nhóm", HttpStatus.NOT_FOUND),

    GROUP_FILE_ALREADY_DELETED(1229, "Tệp nhóm đã bị xóa", HttpStatus.CONFLICT),

    GROUP_FILE_NOT_DELETED(1230, "Tệp nhóm không nằm trong thùng rác", HttpStatus.BAD_REQUEST),

    GROUP_FILE_NOT_IN_GROUP(1231, "Tệp không thuộc nhóm này", HttpStatus.BAD_REQUEST),

    FOLDER_NAME_REQUIRED(1318, "Yêu cầu tên thư mục", HttpStatus.BAD_REQUEST),

    FOLDER_NAME_INVALID(1319, "Tên thư mục không hợp lệ", HttpStatus.BAD_REQUEST),

    FOLDER_ALREADY_EXISTS(1320, "Thư mục đã tồn tại", HttpStatus.CONFLICT),

    FOLDER_NOT_FOUND(1321, "Không tìm thấy thư mục", HttpStatus.NOT_FOUND),

    FOLDER_ACCESS_DENIED(1322, "Không có quyền truy cập thư mục", HttpStatus.FORBIDDEN),

    // ==================== ADMIN ADVANCED ERRORS (14xx) ====================

    // Dùng khi Admin cố tình tự khóa tài khoản của chính mình
    ADMIN_CANNOT_LOCK_SELF(1401, "Quản trị viên không thể tự khóa tài khoản của mình", HttpStatus.BAD_REQUEST),

    // Dùng khi tài khoản đã bị khóa bởi hệ thống/Admin rồi nhưng vẫn cố tình đăng nhập
    ACCOUNT_IS_LOCKED(1402, "Tài khoản đã bị khóa. Vui lòng liên hệ hỗ trợ", HttpStatus.FORBIDDEN),

    // Dùng khi lý do khóa tài khoản gửi lên từ request bị trống
    LOCK_REASON_REQUIRED(1403, "Yêu cầu lý do khóa tài khoản", HttpStatus.BAD_REQUEST),

    // Dùng khi tài khoản đang ở trạng thái bình thường nhưng Admin lại gọi API mở khóa
    ACCOUNT_ALREADY_UNLOCKED(1404, "Tài khoản đã được mở khóa", HttpStatus.CONFLICT);

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
