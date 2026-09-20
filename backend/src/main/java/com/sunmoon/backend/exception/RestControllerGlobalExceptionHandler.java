package com.sunmoon.backend.exception;

import com.sunmoon.backend.dto.response.ResponseData;
import com.sunmoon.backend.exception.customize.CommonException;
import com.sunmoon.backend.exception.customize.DispatchBlockedException;
import com.sunmoon.backend.exception.customize.InvalidFieldException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestControllerAdvice
public class RestControllerGlobalExceptionHandler {

    @ExceptionHandler({ CommonException.class })
    public ResponseEntity<ResponseData<?>> handleCommonException(CommonException e, WebRequest request) {
        return ResponseEntity.status(e.getHttpStatus())
                .body(ResponseData.builder()
                        .status(e.getHttpStatus().value())
                        .messageCode(e.getMessage())
                        // Portal đọc `message` để hiện cho người học. Thiếu dòng này thì câu tiếng Việt
                        // chỉ nằm trong messageCode và người học chỉ thấy "Conflict"/"Not Found".
                        .message(e.getMessage())
                        .data(e.getData())
                        .path(getPath(request))
                        .error(e.getHttpStatus().getReasonPhrase())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseData<?>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            WebRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));
        ResponseData<Map<String, String>> body = ResponseData.<Map<String, String>>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .messageCode("VALIDATION_FAILED")
                .message(fieldErrors.values().stream().filter(m -> m != null && !m.isBlank()).findFirst()
                        .orElse("Dữ liệu gửi lên chưa hợp lệ"))
                .data(fieldErrors)
                .error("Bad Request")
                .path(getPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseData<?>> handleConstraintViolation(ConstraintViolationException ex,
            WebRequest request) {
        List<String> violations = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("violations", violations);
        ResponseData<Map<String, Object>> body = ResponseData.<Map<String, Object>>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .messageCode("CONSTRAINT_VIOLATIONS")
                .data(data)
                .error("Bad Request")
                .path(getPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ResponseData<?>> handleNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        ResponseData<Void> body = ResponseData.<Void>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .messageCode("MALFORMED_JSON_REQUEST")
                .error("Bad Request")
                .path(getPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Thiếu phần multipart, thiếu tham số bắt buộc, hoặc tham số sai kiểu.
     *
     * Đây đều là LỖI CỦA CLIENT. Không bắt riêng thì chúng rơi xuống
     * handleGeneric() và trả về 500 — tức là đổ lỗi cho máy chủ vì một yêu cầu
     * gửi sai, và người phát triển client sẽ đi tìm nhầm chỗ.
     */
    @ExceptionHandler({
            MissingServletRequestPartException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ResponseData<?>> handleBadRequest(Exception ex, WebRequest request) {
        // Dùng instanceof thay cho pattern matching trong switch: dự án biên dịch
        // ở Java 17, cú pháp đó là tính năng xem trước của Java 21.
        String message;
        if (ex instanceof MissingServletRequestPartException e) {
            message = "Thiếu phần dữ liệu bắt buộc: " + e.getRequestPartName();
        } else if (ex instanceof MissingServletRequestParameterException e) {
            message = "Thiếu tham số bắt buộc: " + e.getParameterName();
        } else if (ex instanceof MethodArgumentTypeMismatchException e) {
            message = "Tham số '" + e.getName() + "' sai định dạng";
        } else {
            message = "Yêu cầu không hợp lệ";
        }
        ResponseData<Void> body = ResponseData.<Void>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .messageCode("BAD_REQUEST")
                .message(message)
                .error("Bad Request")
                .path(getPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Client gửi sai Content-Type — ví dụ gửi form thường vào endpoint chỉ nhận
     * multipart. Đây là lỗi của client, trả 415 để họ biết phải sửa ở đâu thay
     * vì thấy 500 rồi đi tìm lỗi trong máy chủ.
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ResponseData<?>> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex, WebRequest request) {
        String supported = ex.getSupportedMediaTypes().stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "));
        ResponseData<Void> body = ResponseData.<Void>builder()
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .messageCode("UNSUPPORTED_MEDIA_TYPE")
                .message("Kiểu dữ liệu gửi lên không được hỗ trợ"
                        + (supported.isBlank() ? "" : ". Endpoint này nhận: " + supported))
                .error("Unsupported Media Type")
                .path(getPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(body);
    }

    /** Tệp vượt quá giới hạn của Spring — trả 413 để client phân biệt với lỗi khác */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ResponseData<?>> handleTooLarge(MaxUploadSizeExceededException ex,
            WebRequest request) {
        ResponseData<Void> body = ResponseData.<Void>builder()
                .status(HttpStatus.PAYLOAD_TOO_LARGE.value())
                .messageCode("PAYLOAD_TOO_LARGE")
                .message("Tệp quá lớn so với giới hạn của máy chủ")
                .error("Payload Too Large")
                .path(getPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ResponseData<?>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex,
            WebRequest request) {
        ResponseData<Void> body = ResponseData.<Void>builder()
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .messageCode("METHOD_NOT_ALLOWED")
                .error("Method Not Allowed")
                .path(getPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseData<?>> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        ResponseData<Void> body = ResponseData.<Void>builder()
                .status(HttpStatus.FORBIDDEN.value())
                .messageCode("ACCESS_DENIED")
                .error("Forbidden")
                .path(getPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ResponseData<?>> handleAuth(AuthenticationException ex, WebRequest request) {
        ResponseData<Void> body = ResponseData.<Void>builder()
                .status(HttpStatus.UNAUTHORIZED.value())
                .messageCode("UNAUTHORIZED")
                .error("Unauthorized")
                .path(getPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    @ExceptionHandler({ EntityNotFoundException.class, NoSuchElementException.class })
    public ResponseEntity<ResponseData<?>> handleNotFound(RuntimeException ex, WebRequest request) {
        ResponseData<Void> body = ResponseData.<Void>builder()
                .status(HttpStatus.NOT_FOUND.value())
                .messageCode("RESOURCE_NOT_FOUND")
                .error("Not Found")
                .path(getPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ResponseData<?>> handleConflict(DataIntegrityViolationException ex, WebRequest request) {
        ResponseData<Void> body = ResponseData.<Void>builder()
                .status(HttpStatus.CONFLICT.value())
                .messageCode("DATA_INTEGRITY_VIOLATION")
                .error("Conflict")
                .path(getPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(DispatchBlockedException.class)
    public ResponseEntity<ResponseData<?>> handleDispatchBlocked(
            DispatchBlockedException ex, WebRequest request) {
        ResponseData<Map<String, Object>> body = ResponseData.<Map<String, Object>>builder()
                .status(HttpStatus.CONFLICT.value())
                .messageCode(ex.getMessage())
                .data(ex.getPreCheckDetails())
                .error("Dispatch Blocked")
                .path(getPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(InvalidFieldException.class)
    public ResponseEntity<ResponseData<?>> handleInvalidField(InvalidFieldException ex, WebRequest request) {
        ResponseData<Void> body = ResponseData.<Void>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .messageCode(ex.getMessage())
                .error("Bad Request")
                .path(getPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ResponseData<?>> handleTransactionSystem(TransactionSystemException ex, WebRequest request) {
        ResponseData<Void> body = ResponseData.<Void>builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .messageCode(ex.getMessage())
                .error("Bad Request")
                .path(getPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseData<?>> handleGeneric(Exception ex, WebRequest request) {
        ResponseData<Void> body = ResponseData.<Void>builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .messageCode("INTERNAL_SERVER_ERROR")
                .error(ex.getMessage())
                .path(getPath(request))
                .build();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    private String getPath(WebRequest request) {
        try {
            if (request instanceof ServletWebRequest servletWebRequest) {
                return servletWebRequest.getRequest().getRequestURI();
            }
        } catch (Exception ignored) {
        }
        return request.getDescription(false);
    }
}
