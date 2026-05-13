package com.example.externalapi.infrastructure.http.interceptor;

/**
 * HTTP 出站响应上下文。
 *
 * <p>该对象只在 2xx 响应后创建，并传给
 * {@link HttpClientInterceptor#afterResponse(HttpClientResponseContext)}。</p>
 */
public class HttpClientResponseContext {

    /**
     * HTTP 方法。
     */
    private final String method;

    /**
     * 实际请求 URL。
     */
    private final String url;

    /**
     * HTTP 状态码。
     */
    private final int statusCode;

    /**
     * 原始响应体字符串。
     */
    private final String responseBody;

    public HttpClientResponseContext(String method, String url, int statusCode, String responseBody) {
        this.method = method;
        this.url = url;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
