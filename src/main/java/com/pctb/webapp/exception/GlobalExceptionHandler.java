package com.pctb.webapp.exception;

import com.pctb.webapp.dto.response.ApiResponse;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
// ================= Class dùng để xử lý , những dòng thông báo lỗi khi có lỗi ============
// Spring gọi các method @ExceptionHandler bằng reflection nên code sẽ không có caller trực tiếp.
@SuppressWarnings("unused")
@RestControllerAdvice
public class GlobalExceptionHandler {

    // Xử lý khi có Exception //
    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse> handlingAppException(AppException ex){
        return buildErrorResponse(ex.getErrorCode());
    }

    // Xử lý khi validation request //
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> handlingMethodArgumentNotValidException(MethodArgumentNotValidException ex){
        FieldError fieldError = ex.getFieldError();
        String enumKey = fieldError == null ? null : fieldError.getDefaultMessage();
        ErrorCode errorCode = resolveErrorCode(enumKey, ErrorCode.REQUEST_BODY_INVALID);
        return buildErrorResponse(errorCode);
    }

    // Xử lý khi thiếu query/path parameter bắt buộc.
    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    ResponseEntity<ApiResponse> handlingMissingServletRequestParameterException(
            MissingServletRequestParameterException ex) {
        return buildErrorResponse(resolveRequestParameterError(ex.getParameterName()));
    }

    // Xử lý khi query/path parameter sai kiểu dữ liệu, ví dụ ttlSeconds=abc.
    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiResponse> handlingMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {
        return buildErrorResponse(resolveRequestParameterError(ex.getName()));
    }

    // Xử lý khi request body bị thiếu hoặc JSON sai format.
    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse> handlingHttpMessageNotReadableException() {
        return buildErrorResponse(ErrorCode.REQUEST_BODY_INVALID);
    }

    // Xử lý lỗi file quá dung lượng trước khi request đi vào UserService.
    @ExceptionHandler(value = MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse> handlingMaxUploadSizeExceededException() {
        return buildErrorResponse(ErrorCode.AVATAR_SIZE_EXCEEDED);
    }

    private ErrorCode resolveRequestParameterError(String parameterName) {
        if ("key".equals(parameterName)) {
            return ErrorCode.KEY_REQUIRED;
        }

        if ("ttlSeconds".equals(parameterName)) {
            return ErrorCode.TTL_INVALID;
        }

        return ErrorCode.REQUEST_PARAMETER_INVALID;
    }

    private ErrorCode resolveErrorCode(String enumKey, ErrorCode fallback) {
        try {
            return ErrorCode.valueOf(enumKey);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return fallback;
        }
    }

    private ResponseEntity<ApiResponse> buildErrorResponse(ErrorCode errorCode) {
        ApiResponse api = new ApiResponse();
        api.setCode(errorCode.getCode());
        api.setMessage(errorCode.getMessage());
        return  ResponseEntity.status(errorCode.getStatusCode()).body(api);
    }
}
