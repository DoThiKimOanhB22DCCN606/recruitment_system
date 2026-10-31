package com.rms.common.exception;

public class RmsException extends RuntimeException {
    private final ErrorCode errorCode;

    public RmsException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public RmsException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
