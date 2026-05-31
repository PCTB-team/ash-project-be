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