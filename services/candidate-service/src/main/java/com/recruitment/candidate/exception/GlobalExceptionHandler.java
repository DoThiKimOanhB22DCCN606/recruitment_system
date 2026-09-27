package com.recruitment.candidate.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req, null);
    }

    @ExceptionHandler(com.rms.common.exception.RmsException.class)
    public ResponseEntity<ErrorResponse> handleRmsException(com.rms.common.exception.RmsException ex, HttpServletRequest req) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case ERR_MISSING_CREDENTIALS, ERR_TOKEN_EXPIRED, ERR_INVALID_TOKEN -> HttpStatus.UNAUTHORIZED;
            case ERR_ACCOUNT_LOCKED, ERR_INSUFFICIENT_PERMISSION, ERR_TENANT_MISMATCH -> HttpStatus.FORBIDDEN;
            case ERR_EMAIL_ALREADY_EXISTS, ERR_DUPLICATE_APPLICATION, ERR_IDEMPOTENCY_CONFLICT,
                 ERR_OFFER_ALREADY_EXISTS, ERR_INVALID_STATE_TRANSITION, ERR_CV_IN_USE -> HttpStatus.CONFLICT;
            case ERR_RESOURCE_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ERR_VALIDATION_FAILED, ERR_OFFER_INVALID_STAGE, ERR_INTERVIEW_PAST_DATE,
                 ERR_CV_LIMIT_EXCEEDED, ERR_INVALID_FILE_TYPE, ERR_FILE_TOO_LARGE,
                 ERR_JOB_NOT_ACCEPTING_APPLICATIONS -> HttpStatus.BAD_REQUEST;
            case ERR_SERVICE_UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return build(status, ex.getMessage(), req, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", req, details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest req) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest req) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), req, null);
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, HttpServletRequest req, List<String> details) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(req.getRequestURI())
                .details(details)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
