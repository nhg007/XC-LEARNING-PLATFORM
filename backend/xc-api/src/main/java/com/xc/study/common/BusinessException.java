package com.xc.study.common;

import org.springframework.http.HttpStatus;

public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode errorCode;

    public BusinessException(String message) {
        this(HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST, message);
    }

    public BusinessException(HttpStatus status, String message) {
        this(status, ErrorCode.BAD_REQUEST, message);
    }

    public BusinessException(HttpStatus status, ErrorCode errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
