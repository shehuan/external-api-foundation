package com.example.externalapi.infrastructure.http.api;

import java.util.Map;
import org.springframework.util.MultiValueMap;

/**
 * 项目统一的出站 HTTP 客户端入口。
 *
 * <p>业务代码调用外部 HTTP 服务时优先使用该接口，不直接散落使用 {@code RestClient} 或
 * {@code RestTemplate}。统一入口便于集中处理超时、请求头、TraceId、日志、异常转换、回调和拦截器扩展。</p>
 */
public interface HttpClientService {

    /**
     * 发送 GET 请求，适合 URL 中包含路径模板变量的场景。
     *
     * <p>示例：{@code get("https://example.com/users/{id}", UserDTO.class, 10001L)}。</p>
     */
    <T> T get(String url, Class<T> responseType, Object... uriVariables);

    /**
     * 发送 GET 请求，同时支持自定义请求头和路径模板变量。
     *
     * <p>常用于外部接口要求传入 {@code Authorization}、{@code X-App-Id}、{@code X-Sign}
     * 等请求头的场景。</p>
     */
    <T> T get(String url, Map<String, String> headers, Class<T> responseType, Object... uriVariables);

    /**
     * 发送 GET 请求，并把 {@code queryParams} 拼接为 query string。
     *
     * <p>{@code null} 值会被忽略；集合、数组会展开为多个同名 query 参数；{@code Optional.empty()}
     * 会被忽略。</p>
     */
    <T> T get(String url, Map<String, ?> queryParams, Class<T> responseType);

    /**
     * 发送 GET 请求，同时支持自定义请求头和 query 参数。
     */
    <T> T get(String url, Map<String, String> headers, Map<String, ?> queryParams, Class<T> responseType);

    /**
     * 发送 POST JSON 请求。
     *
     * <p>{@code Content-Type} 会自动设置为 {@code application/json}。</p>
     */
    <T> T postJson(String url, Object requestBody, Class<T> responseType);

    /**
     * 发送 POST JSON 请求，同时支持自定义请求头。
     */
    <T> T postJson(String url, Map<String, String> headers, Object requestBody, Class<T> responseType);

    /**
     * 发送 POST 表单请求。
     *
     * <p>{@code Content-Type} 会自动设置为 {@code application/x-www-form-urlencoded}。</p>
     */
    <T> T postForm(String url, MultiValueMap<String, String> formData, Class<T> responseType);

    /**
     * 发送 POST 表单请求，同时支持自定义请求头。
     */
    <T> T postForm(String url, Map<String, String> headers, MultiValueMap<String, String> formData,
            Class<T> responseType);

    /**
     * 发送 GET 回调请求。
     *
     * <p>HTTP 调用成功进入 {@code callback.onSuccess}；HTTP 调用失败进入
     * {@code callback.onFailure}。回调方法本身不返回值，适合在回调中更新业务状态。</p>
     */
    <T> void get(String url, Class<T> responseType, HttpClientCallback<T> callback, Object... uriVariables);

    /**
     * 发送 GET 回调请求，同时支持自定义请求头和路径模板变量。
     */
    <T> void get(String url, Map<String, String> headers, Class<T> responseType, HttpClientCallback<T> callback,
            Object... uriVariables);

    /**
     * 发送 GET 回调请求，并把 {@code queryParams} 拼接为 query string。
     */
    <T> void get(String url, Map<String, ?> queryParams, Class<T> responseType, HttpClientCallback<T> callback);

    /**
     * 发送 GET 回调请求，同时支持自定义请求头和 query 参数。
     */
    <T> void get(String url, Map<String, String> headers, Map<String, ?> queryParams, Class<T> responseType,
            HttpClientCallback<T> callback);

    /**
     * 发送 POST JSON 回调请求。
     */
    <T> void postJson(String url, Object requestBody, Class<T> responseType, HttpClientCallback<T> callback);

    /**
     * 发送 POST JSON 回调请求，同时支持自定义请求头。
     */
    <T> void postJson(String url, Map<String, String> headers, Object requestBody, Class<T> responseType,
            HttpClientCallback<T> callback);

    /**
     * 发送 POST 表单回调请求。
     */
    <T> void postForm(String url, MultiValueMap<String, String> formData, Class<T> responseType,
            HttpClientCallback<T> callback);

    /**
     * 发送 POST 表单回调请求，同时支持自定义请求头。
     */
    <T> void postForm(String url, Map<String, String> headers, MultiValueMap<String, String> formData,
            Class<T> responseType, HttpClientCallback<T> callback);
}
