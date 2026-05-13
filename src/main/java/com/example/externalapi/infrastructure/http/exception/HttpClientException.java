package com.example.externalapi.infrastructure.http.exception;

/**
 * HTTP 客户端统一异常。
 *
 * <p>底层网络异常、非 2xx 状态码、响应解析异常都会转换为该异常。业务代码不需要直接依赖
 * Spring 的 {@code RestClientException}。</p>
 */
public class HttpClientException extends RuntimeException {

    /**
     * HTTP 方法。
     */
    private final String method;

    /**
     * 实际请求 URL。
     */
    private final String url;

    /**
     * HTTP 状态码。网络错误或超时时可能为空。
     */
    private final Integer statusCode;

    /**
     * 原始响应体。网络错误或超时时可能为空。
     */
    private final String responseBody;

    /**
     * 统一错误类型，便于业务或日志判断失败原因。
     */
    private final HttpClientErrorType errorType;

    public HttpClientException(String message, String method, String url, Integer statusCode,
            String responseBody, HttpClientErrorType errorType, Throwable cause) {
        super(message, cause);
        this.method = method;
        this.url = url;
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.errorType = errorType;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public HttpClientErrorType getErrorType() {
        return errorType;
    }
}
