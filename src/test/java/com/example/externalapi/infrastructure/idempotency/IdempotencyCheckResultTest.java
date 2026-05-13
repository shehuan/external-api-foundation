package com.example.externalapi.infrastructure.idempotency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * 验证幂等检查结果的工厂方法语义。
 */
class IdempotencyCheckResultTest {

    @Test
    void startedShouldRepresentFirstProcessingRequest() {
        IdempotencyCheckResult result = IdempotencyCheckResult.started();

        assertTrue(result.firstRequest());
        assertEquals(IdempotencyStatus.PROCESSING, result.status());
        assertNull(result.responseBody());
    }

    @Test
    void repeatedShouldKeepExistingStatusAndResponse() {
        IdempotencyCheckResult result = IdempotencyCheckResult.repeated(IdempotencyStatus.SUCCESS, "{\"ok\":true}");

        assertFalse(result.firstRequest());
        assertEquals(IdempotencyStatus.SUCCESS, result.status());
        assertEquals("{\"ok\":true}", result.responseBody());
    }
}
