package com.pctb.webapp.exception;

import com.pctb.webapp.dto.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
// ================= Class dùng để xử lý , những dòng thông báo lỗi khi có lỗi ============
@ControllerAdvice
public class GlobalExceptionHandler {

    // Xử lý khi có Exception //
    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse> handlingAppException(AppException ex){
        ApiResponse api = new ApiResponse();
        ErrorCode errorCode = ex.getErrorCode();
        api.setCode(errorCode.getCode());
        api.setMessage(errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatusCode()).body(api);
    }

    // Xử lý khi validation request //
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> handlingMethodArgumentNotValidException(MethodArgumentNotValidException ex){
        String enumkey = ex.getFieldError().getDefaultMessage();
        ErrorCode errorCode = ErrorCode.valueOf(enumkey);
        ApiResponse api = new ApiResponse();
        api.setCode(errorCode.getCode());
        api.setMessage(errorCode.getMessage());
        return  ResponseEntity.badRequest().body(api);
    }
}
