package com.rms.iam.exception;

import com.rms.common.exception.RmsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import com.rms.common.dto.ApiError;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RmsException.class)
    public ResponseEntity<ApiError> handleRmsException(RmsException ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Correlation-Id");
        if (traceId == null) {
            traceId = request.getHeader("traceId");
        }

        HttpStatus status = switch (ex.getErrorCode()) {
            case ERR_MISSING_CREDENTIALS, ERR_TOKEN_EXPIRED, ERR_INVALID_TOKEN -> HttpStatus.UNAUTHORIZED;
            case ERR_ACCOUNT_LOCKED, ERR_INSUFFICIENT_PERMISSION, ERR_TENANT_MISMATCH -> HttpStatus.FORBIDDEN;
            case ERR_EMAIL_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case ERR_VALIDATION_FAILED -> HttpStatus.BAD_REQUEST;
            case ERR_RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        ApiError apiError = new ApiError(ex.getErrorCode().name(), ex.getMessage(), null, traceId);
        return new ResponseEntity<>(apiError, status);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Correlation-Id");
        if (traceId == null) {
            traceId = request.getHeader("traceId");
        }
        
        List<ApiError.Detail> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new ApiError.Detail(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());
                
        ApiError apiError = new ApiError("ERR_VALIDATION_FAILED", "Validation failed", details, traceId);
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAllOtherExceptions(Exception ex, HttpServletRequest request) {
        String traceId = request.getHeader("X-Correlation-Id");
        if (traceId == null) {
            traceId = request.getHeader("traceId");
        }
        
        ApiError apiError = new ApiError("ERR_INTERNAL_SERVER_ERROR", "An unexpected error occurred", null, traceId);
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
