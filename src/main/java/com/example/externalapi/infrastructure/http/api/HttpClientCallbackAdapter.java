package com.example.externalapi.infrastructure.http.api;

import com.example.externalapi.infrastructure.http.exception.HttpClientException;
import java.util.function.Consumer;

/**
 * 基于 {@link Consumer} 的回调适配器。
 *
 * <p>{@link HttpClientCallback} 有两个抽象方法，不能直接写成单个 lambda。
 * 该适配器允许调用方用两个 {@link Consumer} 简化回调代码。</p>
 */
public record HttpClientCallbackAdapter<T>(
        Consumer<T> success,
        Consumer<HttpClientException> failure
) implements HttpClientCallback<T> {

    /**
     * 使用成功回调和失败回调创建适配器。
     */
    public static <T> HttpClientCallbackAdapter<T> of(Consumer<T> success, Consumer<HttpClientException> failure) {
        return new HttpClientCallbackAdapter<>(success, failure);
    }

    @Override
    public void onSuccess(T response) {
        success.accept(response);
    }

    @Override
    public void onFailure(HttpClientException exception) {
        failure.accept(exception);
    }
}
