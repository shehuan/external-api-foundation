package com.example.externalapi.common.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.example.externalapi.common.error.ErrorCode;
import com.example.externalapi.infrastructure.logging.TraceIdUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * 验证统一响应对象会正确携带业务码、响应数据和当前 TraceId。
 */
class ApiResponseTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void successShouldUseSuccessCodeAndCurrentTraceId() {
        MDC.put(TraceIdUtils.MDC_TRACE_ID, "trace-123");

        ApiResponse<String> response = ApiResponse.success("ok");

        assertEquals(ErrorCode.SUCCESS.getCode(), response.code());
        assertEquals(ErrorCode.SUCCESS.getMessage(), response.message());
        assertEquals("ok", response.data());
        assertEquals("trace-123", response.traceId());
    }

    @Test
    void failureShouldUseGivenErrorCodeAndMessage() {
        MDC.put(TraceIdUtils.MDC_TRACE_ID, "trace-456");

        ApiResponse<Void> response = ApiResponse.failure(ErrorCode.PARAM_INVALID, "bad param");

        assertEquals(ErrorCode.PARAM_INVALID.getCode(), response.code());
        assertEquals("bad param", response.message());
        assertNull(response.data());
        assertEquals("trace-456", response.traceId());
    }
}
