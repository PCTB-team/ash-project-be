package com.pctb.webapp.exception;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@AllArgsConstructor
public enum ErrorCode {
    // Dùng khi email trong yêu cầu bị trống, sai định dạng hoặc vượt quá giới hạn độ dài.
    EMAIL_INVALID(1001, "Email không hợp lệ", HttpStatus.BAD_REQUEST),

    // Dùng khi đăng ký hoặc xác thực đăng ký nhưng email đã tồn tại trong hệ thống.
    EMAIL_ALREADY_EXISTS(1002, "Email đã tồn tại", HttpStatus.CONFLICT),

    // Dùng khi đăng ký hoặc xác thực đăng ký nhưng tên đăng nhập đã tồn tại trong hệ thống.
    USERNAME_ALREADY_EXISTS(1003, "Tên đăng nhập đã tồn tại", HttpStatus.CONFLICT),

    // Dùng khi tên đăng nhập bị trống, không có từ 3 đến 20 ký tự hoặc chứa ký tự đặc biệt.
    USERNAME_INVALID(1004, "Tên đăng nhập phải có từ 3 đến 20 ký tự và không được chứa ký tự đặc biệt", HttpStatus.BAD_REQUEST),

    // Dùng khi mật khẩu đăng ký hoặc đặt lại bị trống, dưới 8 ký tự hoặc thiếu ký tự đặc biệt.
    PASSWORD_INVALID(1005, "Mật khẩu phải có ít nhất 8 ký tự và chứa ít nhất một ký tự đặc biệt", HttpStatus.BAD_REQUEST),

    // Dùng khi mật khẩu xác nhận đăng ký không khớp với mật khẩu.
    CONFIRM_PASSWORD_NOT_MATCH(1006, "Mật khẩu xác nhận không khớp", HttpStatus.BAD_REQUEST),

    // Dùng khi không còn dữ liệu đăng ký tạm trong Redis để gửi lại hoặc xác thực mã OTP đăng ký.
    REGISTER_SESSION_EXPIRED(1007, "Phiên đăng ký đã hết hạn. Vui lòng đăng ký lại", HttpStatus.BAD_REQUEST),

    // Dùng khi email đã gửi mã OTP vượt quá số lần cho phép trong ngày.
    OTP_SEND_LIMIT_EXCEEDED(1008, "Bạn đã vượt quá giới hạn gửi mã OTP", HttpStatus.TOO_MANY_REQUESTS),

    // Dùng khi người dùng yêu cầu gửi lại mã OTP trước khi hết thời gian chờ.
    OTP_RESEND_TOO_SOON(1009, "Vui lòng đợi 60 giây trước khi yêu cầu mã OTP mới", HttpStatus.TOO_MANY_REQUESTS),

    // Dùng khi mã OTP đăng ký bị trống, sai định dạng 6 số hoặc không khớp mã đã lưu.
    OTP_INVALID(1010, "Mã OTP không hợp lệ", HttpStatus.BAD_REQUEST),

    // Dùng khi mã OTP đăng ký đã hết hạn hoặc không còn trong Redis.
    OTP_EXPIRED(1011, "Mã OTP đã hết hạn", HttpStatus.BAD_REQUEST),

    // Dùng khi tài khoản đã xác thực nhưng vẫn yêu cầu gửi lại mã OTP đăng ký.
    ACCOUNT_ALREADY_VERIFIED(1012, "Tài khoản đã được xác thực", HttpStatus.CONFLICT),

    // Dùng khi thông tin đăng nhập bị trống; thông tin này có thể là email hoặc tên đăng nhập.
    IDENTIFIER_REQUIRED(1013, "Vui lòng nhập email hoặc tên đăng nhập", HttpStatus.BAD_REQUEST),

    // Dùng khi mật khẩu đăng nhập bị trống.
    PASSWORD_REQUIRED(1014, "Vui lòng nhập mật khẩu", HttpStatus.BAD_REQUEST),

    // Dùng khi tài khoản tồn tại nhưng chưa xác thực email nên không được đăng nhập hoặc làm mới phiên.
    ACCOUNT_NOT_VERIFIED(1015, "Tài khoản chưa được xác thực", HttpStatus.FORBIDDEN),

    // Dùng khi thông tin đăng nhập hoặc mật khẩu không đúng.
    USERNAME_OR_PASSWORD_INCORRECT(1016, "Tên đăng nhập hoặc mật khẩu không chính xác", HttpStatus.UNAUTHORIZED),

    // Dùng khi số lần đăng nhập sai vượt quá giới hạn trong khoảng thời gian cấu hình.
    LOGIN_ATTEMPTS_EXCEEDED(1017, "Bạn đã đăng nhập sai quá nhiều lần. Vui lòng thử lại sau", HttpStatus.TOO_MANY_REQUESTS),

    // Dùng khi mã làm mới trong yêu cầu bị trống.
    REFRESH_TOKEN_REQUIRED(1018, "Mã làm mới là bắt buộc", HttpStatus.BAD_REQUEST),

    // Dùng khi mã làm mới sai chữ ký, sai loại, không khớp Redis hoặc không cùng người dùng với mã truy cập.
    REFRESH_TOKEN_INVALID(1019, "Mã làm mới không hợp lệ", HttpStatus.UNAUTHORIZED),

    // Dùng khi mã truy cập hoặc mã làm mới đã quá hạn.
    TOKEN_EXPIRED(1020, "Mã xác thực đã hết hạn", HttpStatus.UNAUTHORIZED),

    // Dùng khi đăng xuất nhưng mã làm mới của người dùng không còn trong Redis.
    ACCOUNT_ALREADY_LOGGED_OUT(1021, "Tài khoản đã đăng xuất", HttpStatus.CONFLICT),

    // Dùng khi mã truy cập bị thiếu, trống, sai chữ ký, sai loại hoặc không hợp lệ.
    UNAUTHENTICATED(1022, "Bạn cần đăng nhập để thực hiện thao tác này", HttpStatus.UNAUTHORIZED),

    // Dùng khi gửi mã OTP hoặc đặt lại mật khẩu nhưng email không tồn tại trong hệ thống.
    EMAIL_NOT_EXISTED(1023, "Email không tồn tại trong hệ thống", HttpStatus.NOT_FOUND),

    // Dùng khi mã OTP quên mật khẩu bị trống, sai định dạng 6 số hoặc không khớp mã đã lưu.
    FORGOT_PASSWORD_OTP_INVALID(1024, "Mã OTP xác minh không hợp lệ", HttpStatus.BAD_REQUEST),

    // Dùng khi mã OTP quên mật khẩu đã hết hạn hoặc không còn trong Redis.
    FORGOT_PASSWORD_OTP_EXPIRED(1025, "Mã OTP xác minh đã hết hạn hoặc không tồn tại", HttpStatus.BAD_REQUEST),

    // Dùng khi mã đặt lại mật khẩu bị trống, hết hạn hoặc không tồn tại trong Redis.
    RESET_TOKEN_INVALID(1026, "Phiên đã hết hạn hoặc mã đặt lại mật khẩu không hợp lệ", HttpStatus.UNAUTHORIZED),

    // Dùng khi mật khẩu xác nhận không khớp với mật khẩu mới trong luồng đặt lại mật khẩu.
    RESET_PASSWORD_MISMATCH(1027, "Mật khẩu xác nhận không khớp với mật khẩu mới", HttpStatus.BAD_REQUEST),

    // Dùng khi lưu mật khẩu mới thất bại trong luồng đặt lại mật khẩu.
    RESET_PASSWORD_FAILED(1028, "Không thể cập nhật mật khẩu mới", HttpStatus.INTERNAL_SERVER_ERROR),

    // Dùng khi mã xác thực đăng nhập Google bị trống.
    GOOGLE_TOKEN_REQUIRED(1029, "Mã xác thực Google là bắt buộc", HttpStatus.BAD_REQUEST),

    // Dùng khi mã xác thực Google không thể kiểm tra, không đúng đối tượng nhận hoặc email chưa được xác thực.
    GOOGLE_TOKEN_INVALID(1030, "Mã xác thực Google không hợp lệ", HttpStatus.UNAUTHORIZED),

    // Dùng khi vai trò bắt buộc, ví dụ USER hoặc ADMIN, không tồn tại trong cơ sở dữ liệu.
    ROLE_NOT_FOUND(1031, "Không tìm thấy vai trò", HttpStatus.INTERNAL_SERVER_ERROR),

    // Dùng khi họ và tên đăng ký bị trống hoặc vượt quá giới hạn độ dài.
    FULLNAME_INVALID(1032, "Họ và tên không hợp lệ", HttpStatus.BAD_REQUEST),

    // Dùng khi mã người dùng lấy từ mã xác thực hợp lệ nhưng không tìm thấy người dùng trong cơ sở dữ liệu.
    USER_NOT_FOUND(1204, "Không tìm thấy người dùng", HttpStatus.NOT_FOUND),

    // Dùng khi khóa dữ liệu của giao diện lập trình Redis bị trống.
    KEY_REQUIRED(1101, "Khóa dữ liệu là bắt buộc", HttpStatus.BAD_REQUEST),

    // Dùng khi giá trị cần lưu vào Redis bị trống.
    VALUE_REQUIRED(1102, "Giá trị là bắt buộc", HttpStatus.BAD_REQUEST),

    // Dùng khi thời gian sống của dữ liệu Redis bị trống hoặc nhỏ hơn 1 giây.
    TTL_INVALID(1103, "Thời gian sống phải lớn hơn 0", HttpStatus.BAD_REQUEST),

    // Dùng khi bộ nhớ đệm Redis gặp sự cố kết nối, không thể đọc hoặc ghi dữ liệu tạm thời.
    REDIS_CONNECTION_FAILED(1104, "Không thể kết nối đến dịch vụ bộ nhớ đệm", HttpStatus.INTERNAL_SERVER_ERROR),

    // Dùng khi tệp tải lên bị trống.
    FILE_REQUIRED(1105, "Tệp tải lên không được để trống", HttpStatus.BAD_REQUEST),

    // Dùng khi dung lượng tệp vượt quá giới hạn hệ thống cho phép, ví dụ lớn hơn 5 MB.
    FILE_SIZE_EXCEEDED(1106, "Dung lượng tệp vượt quá giới hạn cho phép", HttpStatus.CONTENT_TOO_LARGE),

    // Dùng khi định dạng tệp không hợp lệ, ví dụ hệ thống chỉ nhận .png hoặc .jpg.
    INVALID_FILE_FORMAT(1107, "Định dạng tệp không được hỗ trợ", HttpStatus.BAD_REQUEST),

    // Dùng khi nội dung yêu cầu bị thiếu hoặc không đọc được JSON.
    REQUEST_BODY_INVALID(1201, "Nội dung yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),

    // Dùng khi tham số truy vấn hoặc tham số đường dẫn bị thiếu, sai kiểu dữ liệu hoặc không ánh xạ được về mã lỗi cụ thể hơn.
    REQUEST_PARAMETER_INVALID(1202, "Tham số yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),

    // Tiêu đề tài liệu bị bỏ trống hoặc chỉ chứa khoảng trắng
    DOCUMENT_TITLE_REQUIRED(1301, "Tiêu đề tài liệu là bắt buộc", HttpStatus.BAD_REQUEST),

    // Người dùng không gửi tệp trong yêu cầu tải lên.
    FILE_REQUIRED_UPLOAD(1302, "Tệp là bắt buộc", HttpStatus.BAD_REQUEST),

    // Kích thước tệp vượt quá giới hạn cho phép của hệ thống.
    FILE_TOO_LARGE(1303, "Tệp vượt quá dung lượng cho phép", HttpStatus.BAD_REQUEST),

    // Định dạng tệp không nằm trong danh sách được hỗ trợ.
// (pdf, docx, png, jpg, jpeg, txt, mp3, mp4, ppt, pptx)
    FILE_TYPE_NOT_SUPPORTED(1304, "Loại tệp không được hỗ trợ", HttpStatus.BAD_REQUEST),

    // Phần mở rộng tệp và loại MIME thực tế không khớp nhau.
// Ví dụ: đổi tên virus.exe thành document.pdf.
    INVALID_MIME_TYPE(1305, "Loại MIME không hợp lệ hoặc phần mở rộng tệp đã bị giả mạo", HttpStatus.BAD_REQUEST),

    // Dung lượng lưu trữ còn lại của người dùng không đủ để tải tệp lên.
    STORAGE_NOT_ENOUGH(1306, "Không đủ dung lượng lưu trữ. Vui lòng nâng cấp gói của bạn", HttpStatus.FORBIDDEN),

    // Đã tồn tại tệp cùng tên và người dùng không chọn chế độ ghi đè.
    FILE_ALREADY_EXISTS(1307, "Tệp đã tồn tại", HttpStatus.CONFLICT),

    // Tải tệp lên thất bại do lỗi hệ thống.
// Ví dụ: lỗi Cloudinary, AWS S3, cơ sở dữ liệu hoặc kết nối mạng.
    UPLOAD_FAILED(1308, "Tải tệp lên thất bại", HttpStatus.INTERNAL_SERVER_ERROR),

    // Không tìm thấy phiên tải lên.
// Có thể phiên đã hết hạn, bị xóa hoặc mã phiên không hợp lệ.
    UPLOAD_SESSION_NOT_FOUND(1309, "Không tìm thấy phiên tải lên", HttpStatus.NOT_FOUND),

    // Không thể tạm dừng quá trình tải lên ở trạng thái hiện tại.
// Ví dụ: quá trình tải lên đã hoàn thành, đã hủy hoặc đang tạm dừng.
    UPLOAD_CANNOT_BE_PAUSED(1310, "Không thể tạm dừng quá trình tải lên", HttpStatus.BAD_REQUEST),

    // Không thể tiếp tục quá trình tải lên ở trạng thái hiện tại.
// Ví dụ: quá trình tải lên đã hoàn thành, đã hủy hoặc chưa từng bị tạm dừng.
    UPLOAD_CANNOT_BE_RESUMED(1311, "Không thể tiếp tục quá trình tải lên", HttpStatus.BAD_REQUEST),

    // Không thể hủy quá trình tải lên ở trạng thái hiện tại.
// Ví dụ: quá trình tải lên đã hoàn thành hoặc đã bị hủy trước đó.
    UPLOAD_CANNOT_BE_CANCELED(1312, "Không thể hủy quá trình tải lên", HttpStatus.BAD_REQUEST),

    DOCUMENT_NOT_FOUND(1313, "Không tìm thấy tài liệu", HttpStatus.NOT_FOUND),

    DOCUMENT_ACCESS_DENIED(1314, "Bạn không có quyền truy cập tài liệu", HttpStatus.FORBIDDEN),

    DOCUMENT_FILE_NAME_REQUIRED(1315, "Tên tệp tài liệu là bắt buộc", HttpStatus.BAD_REQUEST),

    DOCUMENT_EXTENSION_CANNOT_CHANGE(1316, "Không thể thay đổi phần mở rộng của tệp", HttpStatus.BAD_REQUEST),

    DOCUMENT_NOT_IN_TRASH(1317, "Tài liệu không nằm trong thùng rác", HttpStatus.BAD_REQUEST),

    // Các lỗi cập nhật hồ sơ sử dụng mã 12xx theo quy định của nhóm phát triển.
    PROFILE_FULLNAME_INVALID(1203, "Họ và tên không hợp lệ", HttpStatus.BAD_REQUEST),

    OLD_PASSWORD_INCORRECT(1205, "Mật khẩu cũ không chính xác", HttpStatus.BAD_REQUEST),

    NEW_PASSWORD_SAME_AS_OLD(1206, "Mật khẩu mới phải khác mật khẩu cũ", HttpStatus.BAD_REQUEST),

    PROFILE_PASSWORD_INVALID(1207, "Mật khẩu phải có ít nhất 8 ký tự và chứa ít nhất một ký tự đặc biệt", HttpStatus.BAD_REQUEST),

    PROFILE_CONFIRM_PASSWORD_NOT_MATCH(1208, "Mật khẩu xác nhận không khớp với mật khẩu mới", HttpStatus.BAD_REQUEST),

    AVATAR_TYPE_INVALID(1209, "Định dạng ảnh đại diện không hợp lệ. Chỉ hỗ trợ png, jpg và jpeg", HttpStatus.BAD_REQUEST),

    AVATAR_SIZE_EXCEEDED(1210, "Ảnh đại diện không được vượt quá 5 MB", HttpStatus.BAD_REQUEST),

    AVATAR_UPLOAD_FAILED(1211, "Tải ảnh đại diện lên thất bại", HttpStatus.INTERNAL_SERVER_ERROR),

    PROFILE_SCHOOL_INVALID(1212, "Tên trường học không hợp lệ", HttpStatus.BAD_REQUEST),

    // Các lỗi nhóm sử dụng mã 12xx theo quy ước của nhóm phát triển.
    GROUP_NOT_FOUND(1213, "Không tìm thấy nhóm", HttpStatus.NOT_FOUND),

    GROUP_INVITE_DISABLED(1214, "Liên kết mời vào nhóm đã bị vô hiệu hóa", HttpStatus.BAD_REQUEST),

    GROUP_PASSWORD_INCORRECT(1215, "Mật khẩu nhóm không chính xác", HttpStatus.BAD_REQUEST),

    USER_ALREADY_IN_GROUP(1216, "Người dùng đã tham gia nhóm này", HttpStatus.CONFLICT),

    GROUP_JOIN_REQUEST_PENDING(1217, "Yêu cầu tham gia nhóm đang chờ duyệt", HttpStatus.CONFLICT),

    GROUP_MEMBER_NOT_FOUND(1218, "Không tìm thấy thành viên trong nhóm", HttpStatus.NOT_FOUND),

    GROUP_ACCESS_DENIED(1219, "Bạn không có quyền trong nhóm này", HttpStatus.FORBIDDEN),

    GROUP_UPLOAD_NOT_ALLOWED(1220, "Bạn không được phép tải tệp lên nhóm này", HttpStatus.FORBIDDEN),

    GROUP_MEMBER_NOT_APPROVED(1221, "Thành viên chưa được phê duyệt", HttpStatus.FORBIDDEN),

    GROUP_MEMBER_BANNED(1222, "Bạn đã bị cấm khỏi nhóm này", HttpStatus.FORBIDDEN),

    GROUP_MEMBER_NOT_PENDING(1223, "Yêu cầu tham gia nhóm không ở trạng thái chờ duyệt", HttpStatus.BAD_REQUEST),

    GROUP_MEMBER_ALREADY_REJECTED(1224, "Yêu cầu tham gia nhóm đã bị từ chối", HttpStatus.CONFLICT),

    GROUP_INVITE_TOKEN_GENERATION_FAILED(1225, "Không thể tạo mã mời vào nhóm", HttpStatus.INTERNAL_SERVER_ERROR),

    GROUP_LEADER_CANNOT_BE_KICKED(1226, "Không thể xóa trưởng nhóm khỏi nhóm", HttpStatus.BAD_REQUEST),

    GROUP_MEMBER_NOT_APPROVED_TO_KICK(1227, "Chỉ có thể xóa thành viên đã được phê duyệt khỏi nhóm", HttpStatus.BAD_REQUEST),

    GROUP_FILE_NOT_FOUND(1228, "Không tìm thấy tệp trong nhóm", HttpStatus.NOT_FOUND),

    GROUP_FILE_ALREADY_DELETED(1229, "Tệp trong nhóm đã bị xóa", HttpStatus.CONFLICT),

    GROUP_FILE_NOT_DELETED(1230, "Tệp trong nhóm không nằm trong thùng rác", HttpStatus.BAD_REQUEST),

    GROUP_FILE_NOT_IN_GROUP(1231, "Tệp không thuộc nhóm này", HttpStatus.BAD_REQUEST),

    GROUP_PASSWORD_INVALID(1232, "Mật khẩu nhóm không hợp lệ", HttpStatus.BAD_REQUEST),

    GROUP_CONFIRM_PASSWORD_NOT_MATCH(1233, "Mật khẩu xác nhận không khớp với mật khẩu nhóm", HttpStatus.BAD_REQUEST),

    GROUP_NEW_PASSWORD_SAME_AS_OLD(1234, "Mật khẩu nhóm mới phải khác mật khẩu cũ", HttpStatus.BAD_REQUEST),

    GROUP_LEADER_CANNOT_LEAVE(1235, "Trưởng nhóm không thể rời nhóm", HttpStatus.BAD_REQUEST),

    GROUP_MESSAGE_EMPTY(1236, "Tin nhắn nhóm không được để trống", HttpStatus.BAD_REQUEST),

    GROUP_MESSAGE_TOO_LONG(1237, "Tin nhắn nhóm quá dài", HttpStatus.BAD_REQUEST),

    GROUP_LEADER_PERMISSION_REQUIRED(1238, "Chỉ trưởng nhóm mới có quyền thực hiện thao tác này", HttpStatus.FORBIDDEN),

    GROUP_CHAT_NOT_ALLOWED(1239, "Bạn không được phép trò chuyện trong nhóm này", HttpStatus.FORBIDDEN),

    GROUP_LEADER_CANNOT_BE_MUTED(1240, "Không thể tắt quyền trò chuyện của trưởng nhóm", HttpStatus.BAD_REQUEST),

    FOLDER_NAME_REQUIRED(1318, "Tên thư mục là bắt buộc", HttpStatus.BAD_REQUEST),

    FOLDER_NAME_INVALID(1319, "Tên thư mục không hợp lệ", HttpStatus.BAD_REQUEST),

    FOLDER_ALREADY_EXISTS(1320, "Thư mục đã tồn tại", HttpStatus.CONFLICT),

    FOLDER_NOT_FOUND(1321, "Không tìm thấy thư mục", HttpStatus.NOT_FOUND),

    FOLDER_ACCESS_DENIED(1322, "Bạn không có quyền truy cập thư mục", HttpStatus.FORBIDDEN),

    DOCUMENT_INDEXING_FAILED(1323, "Lập chỉ mục tài liệu thất bại", HttpStatus.INTERNAL_SERVER_ERROR),

    // ==================== LỖI QUẢN TRỊ NÂNG CAO (14xx) ====================

    // Dùng khi quản trị viên tự khóa tài khoản của chính mình.
    ADMIN_CANNOT_LOCK_SELF(1401, "Quản trị viên không thể tự khóa tài khoản của mình", HttpStatus.BAD_REQUEST),

    // Dùng khi tài khoản đã bị hệ thống hoặc quản trị viên khóa nhưng vẫn cố gắng đăng nhập.
    ACCOUNT_IS_LOCKED(1402, "Tài khoản đã bị khóa. Vui lòng liên hệ bộ phận hỗ trợ", HttpStatus.FORBIDDEN),

    // Dùng khi lý do khóa tài khoản gửi lên từ yêu cầu bị trống.
    LOCK_REASON_REQUIRED(1403, "Lý do khóa tài khoản là bắt buộc", HttpStatus.BAD_REQUEST),

    // Dùng khi tài khoản đang ở trạng thái bình thường nhưng quản trị viên lại yêu cầu mở khóa.
    ACCOUNT_ALREADY_UNLOCKED(1404, "Tài khoản đã được mở khóa", HttpStatus.CONFLICT),

    // --- ĐỊNH NGHĨA MÃ LỖI NGHIỆP VỤ CHO PHẦN THANH TOÁN VÀ DUNG LƯỢNG CAO CẤP ---
    PLAN_NOT_FOUND(1501, "Gói nâng cấp dung lượng không tồn tại", HttpStatus.NOT_FOUND),

    TRANSACTION_NOT_FOUND(1502, "Mã giao dịch không hợp lệ hoặc không tồn tại", HttpStatus.NOT_FOUND),

    DUPLICATE_TRANSACTION(1503, "Yêu cầu thanh toán trùng lặp đã bị chặn", HttpStatus.BAD_REQUEST),

    UNAUTHORIZED(1504, "Người dùng phải đăng nhập trước khi sử dụng hệ thống", HttpStatus.UNAUTHORIZED),

    FORBIDDEN(1505, "Chỉ tài khoản người dùng mới có thể nâng cấp dung lượng", HttpStatus.FORBIDDEN),

    PAYMENT_GATEWAY_ERROR(1506, "Không thể kết nối đến cổng thanh toán PayOS", HttpStatus.INTERNAL_SERVER_ERROR),

    INVALID_SIGNATURE(1507, "Chữ ký của webhook không hợp lệ", HttpStatus.UNAUTHORIZED),

    AI_QUOTA_EXCEEDED(1601, "Ban da su dung het quota AI hom nay", HttpStatus.TOO_MANY_REQUESTS),

    PLAN_LEVEL_LOW(1508, "Bạn không thể hạ cấp hoặc mua gói có cấp độ thấp hơn hay bằng cấp độ lưu trữ hiện tại", HttpStatus.BAD_REQUEST);

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
