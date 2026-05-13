package com.example.externalapi.infrastructure.http.exception;

/**
 * HTTP 客户端错误类型。
 */
public enum HttpClientErrorType {

    /**
     * 网络连接失败、DNS 失败等底层网络错误。
     */
    NETWORK_ERROR,

    /**
     * 建立连接超时或读取响应超时。
     */
    TIMEOUT,

    /**
     * 外部接口返回非 2xx 状态码。
     */
    HTTP_STATUS_ERROR,

    /**
     * HTTP 状态码成功，但响应体无法反序列化为目标类型。
     */
    RESPONSE_PARSE_ERROR
}
