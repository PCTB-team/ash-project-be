package com.pctb.webapp.exception;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
// ================= Class dùng để xác đụng các error ======================
@AllArgsConstructor
public enum ErrorCode {
    // VD 1 mã code bị lỗi
    USER_EXISTED(1001,"User chưa tồn tại", HttpStatus.BAD_REQUEST);

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
