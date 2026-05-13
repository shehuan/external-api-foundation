package com.example.externalapi.infrastructure.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * 验证 TraceId 的生成、复用和请求头候选值清洗规则。
 */
class TraceIdUtilsTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void getTraceIdShouldReuseExistingMdcValue() {
        MDC.put(TraceIdUtils.MDC_TRACE_ID, "trace-123");

        assertEquals("trace-123", TraceIdUtils.getTraceId());
    }

    @Test
    void getTraceIdShouldGenerateAndStoreWhenMissing() {
        String traceId = TraceIdUtils.getTraceId();

        assertEquals(traceId, MDC.get(TraceIdUtils.MDC_TRACE_ID));
        assertTrue(traceId.matches("[A-Fa-f0-9]{32}"));
    }

    @Test
    void resolveTraceIdShouldAcceptSafeValueAndReplaceUnsafeValue() {
        assertEquals("safe_trace-1", TraceIdUtils.resolveTraceId("safe_trace-1"));

        String generated = TraceIdUtils.resolveTraceId("bad trace id");

        assertNotEquals("bad trace id", generated);
        assertTrue(generated.matches("[A-Fa-f0-9]{32}"));
    }
}
