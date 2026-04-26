package com.xc.study.common;

import java.time.OffsetDateTime;

public record ApiResponse<T>(
        boolean success,
        String code,
        String message,
        T data,
        String traceId,
        OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(
                true,
                ErrorCode.SUCCESS.code(),
                "ok",
                data,
                TraceContext.currentTraceId(),
                OffsetDateTime.now()
        );
    }

    public static <T> ApiResponse<T> fail(String message) {
        return fail(ErrorCode.BAD_REQUEST, message);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return new ApiResponse<>(
                false,
                errorCode.code(),
                message,
                null,
                TraceContext.currentTraceId(),
                OffsetDateTime.now()
        );
    }
}
