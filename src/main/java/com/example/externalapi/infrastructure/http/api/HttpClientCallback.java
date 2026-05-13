package com.example.externalapi.infrastructure.http.api;

import com.example.externalapi.infrastructure.http.exception.HttpClientException;

/**
 * HTTP 调用回调接口。
 *
 * <p>用于“调用方法本身不需要返回值，调用方希望在成功或失败回调中更新业务状态”的场景。
 * HTTP 调用失败会进入 {@link #onFailure(HttpClientException)}；如果回调方法内部抛出业务异常，
 * 该异常会继续向外抛出。</p>
 */
public interface HttpClientCallback<T> {

    /**
     * HTTP 调用成功，并完成响应体反序列化后执行。
     */
    void onSuccess(T response);

    /**
     * HTTP 调用失败，并被统一封装为 {@link HttpClientException} 后执行。
     */
    void onFailure(HttpClientException exception);
}
