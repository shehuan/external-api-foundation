package com.example.externalapi.infrastructure.logging;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * 验证日志脱敏工具只隐藏敏感字段，不破坏非敏感业务字段。
 */
class MaskUtilsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void maskJsonShouldMaskSensitiveFieldsRecursively() {
        // 同时覆盖对象嵌套和数组嵌套，避免只在顶层字段生效。
        String body = """
                {
                  "username": "admin",
                  "password": "123456",
                  "profile": {
                    "email": "admin@example.com"
                  },
                  "items": [
                    {"token": "abc"},
                    {"value": "visible"}
                  ]
                }
                """;

        String masked = MaskUtils.maskJson(objectMapper, body);

        assertEquals(
                "{\"username\":\"admin\",\"password\":\"***\",\"profile\":{\"email\":\"***\"},"
                        + "\"items\":[{\"token\":\"***\"},{\"value\":\"visible\"}]}",
                masked);
    }

    @Test
    void maskJsonShouldReturnDashForBlankBodyAndOriginalForInvalidJson() {
        assertEquals("-", MaskUtils.maskJson(objectMapper, " "));
        assertEquals("not-json", MaskUtils.maskJson(objectMapper, "not-json"));
    }
}
