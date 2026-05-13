package com.example.externalapi.infrastructure.http.interceptor;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP 出站请求上下文。
 *
 * <p>该对象会传给 {@link HttpClientInterceptor#beforeRequest(HttpClientRequestContext)}，
 * 允许拦截器在真正发送请求前修改请求信息。</p>
 */
public class HttpClientRequestContext {

    /**
     * HTTP 方法，例如 GET、POST。
     */
    private final String method;

    /**
     * 最终请求 URL。拦截器可以改写该值，例如追加签名参数或切换灰度地址。
     */
    private String url;

    /**
     * 请求头。使用可变 Map，方便拦截器追加或覆盖请求头。
     */
    private final Map<String, String> headers;

    /**
     * 请求体。GET 通常为空，POST JSON/FORM 会有值。
     */
    private Object requestBody;

    public HttpClientRequestContext(String method, String url, Map<String, String> headers, Object requestBody) {
        this.method = method;
        this.url = url;
        // 拷贝一份请求头，避免拦截器修改调用方传入的原始 Map。
        this.headers = headers == null ? new LinkedHashMap<>() : new LinkedHashMap<>(headers);
        this.requestBody = requestBody;
    }

    public String getMethod() {
        return method;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Object getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(Object requestBody) {
        this.requestBody = requestBody;
    }
}
