package com.pctb.webapp.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

// ===================== Class dùng để gọi enum Error Code =========================
@Data
@AllArgsConstructor
public class AppException extends RuntimeException{
    private ErrorCode errorCode;
}
