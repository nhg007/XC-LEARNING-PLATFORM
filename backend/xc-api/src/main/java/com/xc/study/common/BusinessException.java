package com.xc.study.common;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode errorCode;

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }

    public BusinessException(ErrorCode errorCode, String message) {
        this(HttpStatus.BAD_REQUEST, errorCode, message);
    }

    public BusinessException(HttpStatus status, String message) {
        this(status, ErrorCode.BAD_REQUEST, message);
    }

    public BusinessException(HttpStatus status, ErrorCode errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static BusinessException unauthorized(ErrorCode errorCode, String message) {
        return new BusinessException(HttpStatus.UNAUTHORIZED, errorCode, message);
    }

    public static BusinessException forbidden(ErrorCode errorCode, String message) {
        return new BusinessException(HttpStatus.FORBIDDEN, errorCode, message);
    }

    public static BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, message);
    }

    public static BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, ErrorCode.CONFLICT, message);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
