package com.xc.study.common;

import org.slf4j.MDC;

public final class TraceContext {

    public static final String TRACE_ID = "traceId";
    public static final String TRACE_HEADER = "X-Request-Id";

    private TraceContext() {
    }

    public static String currentTraceId() {
        return MDC.get(TRACE_ID);
    }
}
