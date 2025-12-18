package com.example.duantotnghiep.Exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;

import javax.net.ssl.SSLException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeoutException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    // ===== Helpers =====
    private String pathOf(WebRequest request) {
        if (request instanceof ServletWebRequest swr) {
            return swr.getRequest().getRequestURI();
        }
        return "";
    }

    // =========================================================
    // ===============  VOUCHER: HANDLERS MỚI  =================
    // =========================================================

    /**
     * 1) Voucher đang áp dụng đã bị xoá/tắt/hết hạn trong lúc tính tiền ở POS
     *    Service ném VoucherInvalidException → trả 409 + {code, message}
     */
    @ExceptionHandler(VoucherInvalidException.class)
    public ResponseEntity<Map<String, Object>> handleVoucherInvalid(VoucherInvalidException ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", "VOUCHER_REMOVED");
        body.put("message", ex.getMessage() != null
                ? ex.getMessage()
                : "Voucher đang áp dụng đã bị xoá/tắt hoặc hết hạn, hệ thống đã tự bỏ voucher khỏi hoá đơn.");
        body.put("path", pathOf(request));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body); // 409
    }

    /**
     * 2) Xoá voucher lần 2 ở Admin
     *    Service ném IllegalStateException("Voucher này đã bị xoá trước đó.")
     *    → trả 409 + {code, message}
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex, WebRequest request) {
        String msg = ex.getMessage();
        if (msg != null && msg.contains("đã bị xoá trước đó")) {
            Map<String, Object> body = new HashMap<>();
            body.put("code", "VOUCHER_ALREADY_DELETED");
            body.put("message", msg);
            body.put("path", pathOf(request));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(body); // 409
        }
        // Không phải case voucher → để handler RuntimeException chung xử lý
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("message", msg != null ? msg : "Trạng thái không hợp lệ");
        fallback.put("path", pathOf(request));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(fallback);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage()); // Lấy message từ exception
        return ResponseEntity.badRequest().body(error); // Trả HTTP 400
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message, WebRequest request) {
        ApiError body = ApiError.basic(status, message, pathOf(request));
        return ResponseEntity.status(status).body(body);
    }

    private ResponseEntity<ApiError> buildValidation(HttpStatus status, String message,
                                                     List<ApiError.FieldViolation> violations, WebRequest request) {
        ApiError body = ApiError.withViolations(status, message, pathOf(request), violations);
        return ResponseEntity.status(status).body(body);
    }

    // ===== Validation: @Valid trên @RequestBody =====
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, WebRequest request) {
        List<ApiError.FieldViolation> violations = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            violations.add(new ApiError.FieldViolation(
                    fe.getField(),
                    fe.getRejectedValue() == null ? null : String.valueOf(fe.getRejectedValue()),
                    fe.getDefaultMessage()
            ));
        }
        return buildValidation(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", violations, request);
    }

    // ===== Validation: @Valid trên @ModelAttribute / query params =====
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiError> handleBindException(BindException ex, WebRequest request) {
        List<ApiError.FieldViolation> violations = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            violations.add(new ApiError.FieldViolation(
                    fe.getField(),
                    fe.getRejectedValue() == null ? null : String.valueOf(fe.getRejectedValue()),
                    fe.getDefaultMessage()
            ));
        }
        return buildValidation(HttpStatus.BAD_REQUEST, "Tham số không hợp lệ", violations, request);
    }

    // ===== Sai kiểu tham số =====
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Tham số '" + ex.getName() + "' không đúng kiểu", request);
    }

    // ===== Thiếu tham số =====
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Thiếu tham số bắt buộc: " + ex.getParameterName(), request);
    }

    // ===== JSON/Body không đọc được =====
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Payload không hợp lệ hoặc sai định dạng JSON", request);
    }

    // ===== Không tìm thấy handler (404) =====
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiError> handleNoHandler(NoHandlerFoundException ex, WebRequest request) {
        return build(HttpStatus.NOT_FOUND, "Không tìm thấy tài nguyên", request);
    }

    // ===== Sai method HTTP =====
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Phương thức không được hỗ trợ", request);
    }

    // ===== Sai/không hỗ trợ Content-Type hoặc Accept =====
    @ExceptionHandler({HttpMediaTypeNotSupportedException.class, HttpMediaTypeNotAcceptableException.class})
    public ResponseEntity<ApiError> handleMediaType(Exception ex, WebRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Loại nội dung không được hỗ trợ", request);
    }

    // ===== Lỗi ràng buộc DB (duy nhất, FK, v.v.) =====
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, WebRequest request) {
        return build(HttpStatus.CONFLICT, "Dữ liệu vi phạm ràng buộc", request);
    }

    // ===== ResponseStatusException từ service/controller =====
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatus(ResponseStatusException ex, WebRequest request) {
        HttpStatusCode code = ex.getStatusCode();
        HttpStatus status = (code instanceof HttpStatus hs) ? hs : HttpStatus.valueOf(code.value());
        String message = Objects.requireNonNullElse(ex.getReason(), status.getReasonPhrase());
        return build(status, message, request);
    }

    // ===== 401/403 (nếu dùng Spring Security) =====
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> handleAuth(AuthenticationException ex, WebRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Chưa xác thực hoặc token không hợp lệ", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        return build(HttpStatus.FORBIDDEN, "Không có quyền truy cập", request);
    }

    // ===== Upload quá dung lượng =====
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUpload(MaxUploadSizeExceededException ex, WebRequest request) {
        return build(HttpStatus.PAYLOAD_TOO_LARGE, "Tệp tải lên vượt quá dung lượng cho phép", request);
    }

    // ======== LỖI TÍCH HỢP DỊCH VỤ BÊN THỨ 3 (THIRD-PARTY) ========
    // Có response từ đối tác (HTTP status & body)
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<ApiError> handleUpstreamResponse(RestClientResponseException ex, WebRequest request) {
        int upstream = ex.getRawStatusCode();
        HttpStatus status = (upstream >= 500) ? HttpStatus.SERVICE_UNAVAILABLE : HttpStatus.BAD_GATEWAY;
        String msg = "Lỗi dịch vụ đối tác (" + upstream + ")";
        return build(status, msg, request);
    }

    // Lỗi client/IO khi gọi đối tác (không có response)
    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiError> handleUpstreamClient(RestClientException ex, WebRequest request) {
        Throwable cause = ex.getCause();
        if (cause instanceof SocketTimeoutException || cause instanceof TimeoutException) {
            return build(HttpStatus.GATEWAY_TIMEOUT, "Hết thời gian chờ khi gọi dịch vụ đối tác", request);
        }
        if (cause instanceof ConnectException || cause instanceof UnknownHostException) {
            return build(HttpStatus.SERVICE_UNAVAILABLE, "Không thể kết nối dịch vụ đối tác", request);
        }
        if (cause instanceof SSLException) {
            return build(HttpStatus.BAD_GATEWAY, "Lỗi SSL khi gọi dịch vụ đối tác", request);
        }
        return build(HttpStatus.BAD_GATEWAY, "Không thể gọi dịch vụ đối tác", request);
    }

    // Timeout nổi trực tiếp
    @ExceptionHandler({SocketTimeoutException.class, TimeoutException.class})
    public ResponseEntity<ApiError> handleTimeouts(Exception ex, WebRequest request) {
        return build(HttpStatus.GATEWAY_TIMEOUT, "Hết thời gian chờ khi gọi dịch vụ đối tác", request);
    }

    // Lỗi kết nối nổi trực tiếp
    @ExceptionHandler({ConnectException.class, UnknownHostException.class})
    public ResponseEntity<ApiError> handleConnectivity(Exception ex, WebRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Không thể kết nối dịch vụ đối tác", request);
    }

    // Lỗi SSL nổi trực tiếp
    @ExceptionHandler(SSLException.class)
    public ResponseEntity<ApiError> handleSSL(SSLException ex, WebRequest request) {
        return build(HttpStatus.BAD_GATEWAY, "Lỗi SSL khi gọi dịch vụ đối tác", request);
    }

    // ===== IllegalArgument/BusinessException =====
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArg(IllegalArgumentException ex, WebRequest request) {
        return build(HttpStatus.BAD_REQUEST,
                ex.getMessage() != null ? ex.getMessage() : "Tham số không hợp lệ",
                request);
    }

    // ===== Chặn cuối: 500 =====
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAll(Exception ex, WebRequest request) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống", request);
    }

    // ===== DTO lỗi =====
    public static class ApiError {
        private Instant timestamp;
        private int status;
        private String error;
        private String message;
        private String path;
        private List<FieldViolation> errors;

        public ApiError() {}

        private ApiError(Instant timestamp, int status, String error, String message, String path, List<FieldViolation> errors) {
            this.timestamp = timestamp;
            this.status = status;
            this.error = error;
            this.message = message;
            this.path = path;
            this.errors = errors;
        }

        public static ApiError basic(HttpStatus status, String message, String path) {
            return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, null);
        }

        public static ApiError withViolations(HttpStatus status, String message, String path, List<FieldViolation> errors) {
            return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, path, errors);
        }

        // getters/setters
        public Instant getTimestamp() { return timestamp; }
        public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public List<FieldViolation> getErrors() { return errors; }
        public void setErrors(List<FieldViolation> errors) { this.errors = errors; }

        public static class FieldViolation {
            private String field;
            private String rejectedValue;
            private String message;

            public FieldViolation() {}
            public FieldViolation(String field, String rejectedValue, String message) {
                this.field = field;
                this.rejectedValue = rejectedValue;
                this.message = message;
            }
            public String getField() { return field; }
            public void setField(String field) { this.field = field; }
            public String getRejectedValue() { return rejectedValue; }
            public void setRejectedValue(String rejectedValue) { this.rejectedValue = rejectedValue; }
            public String getMessage() { return message; }
            public void setMessage(String message) { this.message = message; }
        }
    }
}
