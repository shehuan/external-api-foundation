package com.example.externalapi.infrastructure.http.interceptor;

import com.example.externalapi.infrastructure.http.exception.HttpClientException;

/**
 * HTTP 出站异常上下文。
 *
 * <p>该对象在 HTTP 调用失败后创建，并传给
 * {@link HttpClientInterceptor#onError(HttpClientErrorContext)}。</p>
 */
public class HttpClientErrorContext {

    /**
     * HTTP 方法。
     */
    private final String method;

    /**
     * 实际请求 URL。
     */
    private final String url;

    /**
     * 统一封装后的 HTTP 客户端异常。
     */
    private final HttpClientException exception;

    public HttpClientErrorContext(String method, String url, HttpClientException exception) {
        this.method = method;
        this.url = url;
        this.exception = exception;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public HttpClientException getException() {
        return exception;
    }
}
