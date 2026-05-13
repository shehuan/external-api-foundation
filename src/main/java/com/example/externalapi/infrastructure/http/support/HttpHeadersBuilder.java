package com.example.externalapi.infrastructure.http.support;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP 请求头构建工具。
 *
 * <p>用于减少业务代码中反复手写 {@code Authorization}、{@code X-App-Id}、
 * {@code X-Trace-Id} 等常见请求头。</p>
 */
public final class HttpHeadersBuilder {

    private final Map<String, String> headers = new LinkedHashMap<>();

    private HttpHeadersBuilder() {
    }

    public static HttpHeadersBuilder create() {
        return new HttpHeadersBuilder();
    }

    /**
     * 添加普通请求头。{@code name} 为空或 {@code value} 为 {@code null} 时忽略。
     */
    public HttpHeadersBuilder header(String name, String value) {
        if (name != null && !name.isBlank() && value != null) {
            headers.put(name, value);
        }
        return this;
    }

    /**
     * 添加 Bearer Token。
     *
     * <p>调用方可传裸 token，也可传已经带 {@code Bearer } 前缀的完整值。</p>
     */
    public HttpHeadersBuilder bearerToken(String token) {
        if (token != null && !token.isBlank()) {
            header("Authorization", token.startsWith("Bearer ") ? token : "Bearer " + token);
        }
        return this;
    }

    /**
     * 添加调用方应用标识。
     */
    public HttpHeadersBuilder appId(String appId) {
        return header("X-App-Id", appId);
    }

    /**
     * 添加 TraceId。通常不需要手动设置，{@code HttpClientService} 会自动透传当前 TraceId。
     */
    public HttpHeadersBuilder traceId(String traceId) {
        return header("X-Trace-Id", traceId);
    }

    /**
     * 构建不可变 Map，避免构建后继续被外部修改。
     */
    public Map<String, String> build() {
        return Map.copyOf(headers);
    }
}
