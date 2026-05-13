package com.example.externalapi.infrastructure.logging;

import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;

public final class TraceIdUtils {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String MDC_TRACE_ID = "traceId";
    private static final Pattern TRACE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    private TraceIdUtils() {
    }

    public static String getTraceId() {
        String traceId = MDC.get(MDC_TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = generateTraceId();
            MDC.put(MDC_TRACE_ID, traceId);
        }
        return traceId;
    }

    public static String resolveTraceId(String candidate) {
        if (candidate != null && TRACE_ID_PATTERN.matcher(candidate).matches()) {
            return candidate;
        }
        return generateTraceId();
    }

    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
