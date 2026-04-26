package com.xc.study.common;

public enum ErrorCode {
    SUCCESS("SUCCESS"),
    BAD_REQUEST("BAD_REQUEST"),
    VALIDATION_ERROR("VALIDATION_ERROR"),
    UNAUTHORIZED("UNAUTHORIZED"),
    FORBIDDEN("FORBIDDEN"),
    NOT_FOUND("NOT_FOUND"),
    CONFLICT("CONFLICT"),
    METHOD_NOT_ALLOWED("METHOD_NOT_ALLOWED"),
    INTERNAL_ERROR("INTERNAL_ERROR");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
