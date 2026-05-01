package com.doomsday.game.common;

import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Object>> handleApiException(ApiException ex) {
        HttpStatus status = switch (ex.getCode()) {
            case "BAD_REQUEST" -> HttpStatus.BAD_REQUEST;
            case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
            case "CONFLICT_VERSION" -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };

        return ResponseEntity.status(status)
                .body(ApiResponse.fail(ex.getCode(), ex.getMessage(), ex.getData(), traceId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail("BAD_REQUEST", "validation failed", ex.getBindingResult().toString(), traceId()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail("INTERNAL_ERROR", ex.getMessage(), null, traceId()));
    }

    private String traceId() {
        return "tr_" + UUID.randomUUID();
    }
}
