package com.example.externalapi.infrastructure.http.interceptor;

import com.example.externalapi.infrastructure.http.exception.HttpClientException;

/**
 * HTTP 客户端拦截器扩展点。
 *
 * <p>用于在不修改主流程的情况下扩展请求签名、动态 token、统一业务请求头、响应验签、
 * 业务码检查、失败审计等能力。多个拦截器按 {@link #order()} 从小到大执行。</p>
 */
public interface HttpClientInterceptor {

    /**
     * 请求发送前执行。
     *
     * <p>可修改 URL、请求头和请求体。典型用途是追加签名、认证信息、租户标识或应用标识。</p>
     */
    default void beforeRequest(HttpClientRequestContext context) {
    }

    /**
     * 收到 2xx 响应后执行。
     *
     * <p>可读取原始响应体做验签、审计或业务状态码检查。当前版本不支持直接改写响应体。</p>
     */
    default void afterResponse(HttpClientResponseContext context) {
    }

    /**
     * HTTP 调用失败后执行。
     *
     * <p>可记录失败原因、更新业务状态或发送告警。普通调用仍会继续抛出
     * {@link HttpClientException}；回调调用会继续进入 {@code callback.onFailure}。</p>
     */
    default void onError(HttpClientErrorContext context) {
    }

    /**
     * 拦截器执行顺序，数值越小越先执行。
     */
    default int order() {
        return 0;
    }
}
