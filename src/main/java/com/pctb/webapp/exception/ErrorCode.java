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

    IDENTIFIER_REQUIRED(1017, "Identifier is required", HttpStatus.BAD_REQUEST),
    PASSWORD_REQUIRED(1018, "Password is required", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(1019, "Invalid email/username or password", HttpStatus.UNAUTHORIZED),
    ACCOUNT_NOT_VERIFIED(1020, "Account is not verified", HttpStatus.FORBIDDEN),
    LOGIN_ATTEMPTS_EXCEEDED(1021, "Too many login attempts. Please try again later", HttpStatus.TOO_MANY_REQUESTS),
    UNAUTHORIZED(1022, "Unauthorized", HttpStatus.UNAUTHORIZED),
    ACCESS_TOKEN_INVALID(1023, "Access token is invalid", HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID(1024, "Refresh token is invalid", HttpStatus.BAD_REQUEST),
    TOKEN_ALREADY_LOGGED_OUT(1025, "Token already logged out", HttpStatus.CONFLICT),

    REDIS_KEY_INVALID(1101, "Redis key is invalid", HttpStatus.BAD_REQUEST),
    REDIS_VALUE_INVALID(1102, "Redis value is invalid", HttpStatus.BAD_REQUEST),
    REDIS_TTL_INVALID(1103, "TTL must be greater than 0", HttpStatus.BAD_REQUEST),
    REDIS_KEY_NOT_FOUND(1104, "Redis key not found", HttpStatus.NOT_FOUND);

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
