package com.pctb.webapp.exception;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@AllArgsConstructor
public enum ErrorCode {
    EMAIL_INVALID(1001, "Email is invalid", HttpStatus.BAD_REQUEST),
    EMAIL_ALREADY_EXISTS(1002, "Email already exists", HttpStatus.CONFLICT),
    USERNAME_ALREADY_EXISTS(1003, "Username already exists", HttpStatus.CONFLICT),
    USERNAME_INVALID(1004, "Username must be between 3 and 20 characters and contain no special characters", HttpStatus.BAD_REQUEST),
    PASSWORD_INVALID(1005, "Password must be at least 8 characters and contain at least 1 special character", HttpStatus.BAD_REQUEST),
    CONFIRM_PASSWORD_NOT_MATCH(1006, "Confirm password does not match", HttpStatus.BAD_REQUEST),

    REGISTER_SESSION_EXPIRED(1007, "Registration session expired. Please register again", HttpStatus.BAD_REQUEST),
    OTP_SEND_LIMIT_EXCEEDED(1008, "OTP send limit exceeded", HttpStatus.TOO_MANY_REQUESTS),
    OTP_RESEND_TOO_SOON(1009, "Please wait 60 seconds before requesting a new OTP", HttpStatus.TOO_MANY_REQUESTS),
    OTP_INVALID(1010, "OTP is invalid", HttpStatus.BAD_REQUEST),
    OTP_EXPIRED(1011, "OTP has expired", HttpStatus.BAD_REQUEST),
    ACCOUNT_ALREADY_VERIFIED(1012, "Account already verified", HttpStatus.CONFLICT),
    USERNAME_NOT_EXISTED(1013, "Username is not existed", HttpStatus.BAD_REQUEST),
    PASSWORD_NOT_EXISTED(1014, "Password is not existed", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_VERIFIED(1015, "Account is not verified", HttpStatus.FORBIDDEN),
    USERNAME_OR_PASSWORD_INCORRECT(1016, "Username or password is incorrect", HttpStatus.UNAUTHORIZED),
    LOGIN_ATTEMPTS_EXCEEDED(1017, "Login attempts exceeded. Please try again later", HttpStatus.TOO_MANY_REQUESTS),
    REFRESH_TOKEN_REQUIRED(1018, "Refresh token is required", HttpStatus.BAD_REQUEST),
    REFRESH_TOKEN_INVALID(1019, "Refresh token is invalid", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(1020, "Token has expired", HttpStatus.UNAUTHORIZED),
    ACCOUNT_ALREADY_LOGGED_OUT(1021, "Account already logged out", HttpStatus.CONFLICT),
    UNAUTHENTICATED(1022, "Unauthenticated", HttpStatus.UNAUTHORIZED),

    EMAIL_NOT_EXISTED(1020, "Email does not exist in the system", HttpStatus.NOT_FOUND),
    FORGOT_PASSWORD_OTP_INVALID(1021, "Invalid OTP verification code", HttpStatus.BAD_REQUEST),
    FORGOT_PASSWORD_OTP_EXPIRED(1022, "OTP code has expired or does not exist", HttpStatus.BAD_REQUEST),
    RESET_TOKEN_INVALID(1023, "Session has expired or password reset token is invalid", HttpStatus.UNAUTHORIZED),

    // Thêm các mã lỗi này vào dưới RESET_TOKEN_INVALID(1023, ...)
    RESET_PASSWORD_MISMATCH(1024, "Confirm password does not match new password", HttpStatus.BAD_REQUEST),
    RESET_PASSWORD_FAILED(1025, "Failed to update new password", HttpStatus.INTERNAL_SERVER_ERROR),

    KEY_REQUIRED(1101, "Key is required", HttpStatus.BAD_REQUEST),

    VALUE_REQUIRED(1102, "Value is required", HttpStatus.BAD_REQUEST),

    TTL_INVALID(1103, "TTL must be greater than 0", HttpStatus.BAD_REQUEST);

    private int code;

    private String message;
    private HttpStatusCode statusCode;

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
