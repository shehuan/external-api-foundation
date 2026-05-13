package com.example.externalapi.infrastructure.web;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * 验证 Web 路径匹配和 OPTIONS 请求判断逻辑。
 */
class PathMatchUtilsTest {

    @Test
    void matchesAnyShouldSupportAntPatterns() {
        assertTrue(PathMatchUtils.matchesAny(List.of("/api/**", "/health"), "/api/orders/1"));
        assertTrue(PathMatchUtils.matchesAny(List.of("/api/**", "/health"), "/health"));
        assertFalse(PathMatchUtils.matchesAny(List.of("/admin/**"), "/api/orders/1"));
        assertFalse(PathMatchUtils.matchesAny(List.of(), "/api/orders/1"));
        assertFalse(PathMatchUtils.matchesAny(null, "/api/orders/1"));
    }

    @Test
    void isOptionsShouldIgnoreCase() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("options");

        assertTrue(PathMatchUtils.isOptions(request));

        request.setMethod("GET");

        assertFalse(PathMatchUtils.isOptions(request));
    }
}
