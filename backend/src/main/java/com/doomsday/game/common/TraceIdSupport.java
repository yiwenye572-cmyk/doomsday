package com.doomsday.game.common;

import java.util.UUID;
import org.slf4j.MDC;

public final class TraceIdSupport {

    public static final String TRACE_HEADER = "X-Trace-Id";
    public static final String TRACE_ATTRIBUTE = "traceId";
    public static final String TRACE_MDC_KEY = "traceId";

    private TraceIdSupport() {
    }

    public static String ensureTraceId(String incoming) {
        if (incoming != null && !incoming.isBlank()) {
            return incoming.trim();
        }
        return "tr_" + UUID.randomUUID();
    }

    public static String currentTraceId() {
        String value = MDC.get(TRACE_MDC_KEY);
        if (value == null || value.isBlank()) {
            return ensureTraceId(null);
        }
        return value;
    }
}
