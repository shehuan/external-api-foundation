package com.example.externalapi.infrastructure.http.support;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * HTTP URL 构建工具。
 *
 * <p>专门处理 GET query 参数拼接，避免业务代码手写 {@code ?a={a}&b={b}} 这类易错字符串。</p>
 */
public final class HttpClientUriBuilder {

    private HttpClientUriBuilder() {
    }

    public static String appendQueryParams(String url, Map<String, ?> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return url;
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        queryParams.forEach((key, value) -> appendQueryParam(builder, key, value));
        return builder.build().encode().toUriString();
    }

    private static void appendQueryParam(UriComponentsBuilder builder, String key, Object value) {
        if (key == null || key.isBlank() || value == null) {
            return;
        }
        if (value instanceof Optional<?> optional) {
            optional.ifPresent(item -> appendQueryParam(builder, key, item));
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            Iterator<?> iterator = iterable.iterator();
            while (iterator.hasNext()) {
                appendQueryParam(builder, key, iterator.next());
            }
            return;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                appendQueryParam(builder, key, Array.get(value, index));
            }
            return;
        }
        builder.queryParam(key, value);
    }
}
