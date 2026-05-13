package com.example.externalapi.infrastructure.http.support;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * GET query 参数构建测试。
 *
 * <p>只测试 URL 拼接规则，不发起真实 HTTP 调用。</p>
 */
class HttpClientUriBuilderTest {

    @Test
    void appendQueryParamsShouldAppendAndEncodeNormalValues() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("userId", 10001L);
        params.put("status", "PAID");
        params.put("keyword", "hello world");

        String url = HttpClientUriBuilder.appendQueryParams("https://example.com/orders", params);

        assertEquals("https://example.com/orders?userId=10001&status=PAID&keyword=hello%20world", url);
    }

    @Test
    void appendQueryParamsShouldKeepExistingQueryAndSkipNullValues() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("status", "PAID");
        params.put("empty", null);

        String url = HttpClientUriBuilder.appendQueryParams("https://example.com/orders?source=api", params);

        assertEquals("https://example.com/orders?source=api&status=PAID", url);
    }

    @Test
    void appendQueryParamsShouldSupportMultiValueParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("status", List.of("PAID", "CREATED"));
        params.put("type", new String[] {"NORMAL", "VIP"});

        String url = HttpClientUriBuilder.appendQueryParams("https://example.com/orders", params);

        assertEquals("https://example.com/orders?status=PAID&status=CREATED&type=NORMAL&type=VIP", url);
    }

    @Test
    void appendQueryParamsShouldSupportOptionalParams() {
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("userId", Optional.of(10001L));
        params.put("status", Optional.empty());

        String url = HttpClientUriBuilder.appendQueryParams("https://example.com/orders", params);

        assertEquals("https://example.com/orders?userId=10001", url);
    }
}
