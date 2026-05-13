package com.example.externalapi.infrastructure.http.core;

import com.example.externalapi.infrastructure.http.api.HttpClientCallback;
import com.example.externalapi.infrastructure.http.api.HttpClientService;
import com.example.externalapi.infrastructure.http.config.HttpClientProperties;
import com.example.externalapi.infrastructure.http.exception.HttpClientErrorType;
import com.example.externalapi.infrastructure.http.exception.HttpClientException;
import com.example.externalapi.infrastructure.http.interceptor.HttpClientErrorContext;
import com.example.externalapi.infrastructure.http.interceptor.HttpClientInterceptor;
import com.example.externalapi.infrastructure.http.interceptor.HttpClientRequestContext;
import com.example.externalapi.infrastructure.http.interceptor.HttpClientResponseContext;
import com.example.externalapi.infrastructure.http.support.HttpClientUriBuilder;
import com.example.externalapi.infrastructure.logging.MaskUtils;
import com.example.externalapi.infrastructure.logging.TraceIdUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestBodySpec;
import org.springframework.web.client.RestClientException;

/**
 * {@link HttpClientService} 的默认实现。
 *
 * <p>主流程包括：构造请求上下文、执行请求拦截器、发送 HTTP 请求、读取原始响应体、
 * 执行响应拦截器、反序列化响应、统一异常转换和打印出站日志。</p>
 */
@Service
public class DefaultHttpClientService implements HttpClientService {

    private static final Logger log = LoggerFactory.getLogger("HTTP_OUT");

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final HttpClientProperties properties;
    private final List<HttpClientInterceptor> interceptors;

    public DefaultHttpClientService(RestClient restClient, ObjectMapper objectMapper, HttpClientProperties properties,
            List<HttpClientInterceptor> interceptors) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        // 拦截器在构造阶段排序，避免每次请求重复排序。
        this.interceptors = interceptors == null
                ? List.of()
                : interceptors.stream().sorted(Comparator.comparingInt(HttpClientInterceptor::order)).toList();
    }

    @Override
    public <T> T get(String url, Class<T> responseType, Object... uriVariables) {
        return execute(HttpMethod.GET, url, null, null, null, responseType, uriVariables);
    }

    @Override
    public <T> T get(String url, Map<String, String> headers, Class<T> responseType, Object... uriVariables) {
        return execute(HttpMethod.GET, url, null, null, headers, responseType, uriVariables);
    }

    @Override
    public <T> T get(String url, Map<String, ?> queryParams, Class<T> responseType) {
        return execute(HttpMethod.GET, HttpClientUriBuilder.appendQueryParams(url, queryParams),
                null, null, null, responseType);
    }

    @Override
    public <T> T get(String url, Map<String, String> headers, Map<String, ?> queryParams, Class<T> responseType) {
        return execute(HttpMethod.GET, HttpClientUriBuilder.appendQueryParams(url, queryParams),
                null, null, headers, responseType);
    }

    @Override
    public <T> T postJson(String url, Object requestBody, Class<T> responseType) {
        return execute(HttpMethod.POST, url, MediaType.APPLICATION_JSON, requestBody, null, responseType);
    }

    @Override
    public <T> T postJson(String url, Map<String, String> headers, Object requestBody, Class<T> responseType) {
        return execute(HttpMethod.POST, url, MediaType.APPLICATION_JSON, requestBody, headers, responseType);
    }

    @Override
    public <T> T postForm(String url, MultiValueMap<String, String> formData, Class<T> responseType) {
        return execute(HttpMethod.POST, url, MediaType.APPLICATION_FORM_URLENCODED, formData, null, responseType);
    }

    @Override
    public <T> T postForm(String url, Map<String, String> headers, MultiValueMap<String, String> formData,
            Class<T> responseType) {
        return execute(HttpMethod.POST, url, MediaType.APPLICATION_FORM_URLENCODED, formData, headers, responseType);
    }

    @Override
    public <T> void get(String url, Class<T> responseType, HttpClientCallback<T> callback, Object... uriVariables) {
        executeWithCallback(callback, () -> get(url, responseType, uriVariables));
    }

    @Override
    public <T> void get(String url, Map<String, String> headers, Class<T> responseType,
            HttpClientCallback<T> callback, Object... uriVariables) {
        executeWithCallback(callback, () -> get(url, headers, responseType, uriVariables));
    }

    @Override
    public <T> void get(String url, Map<String, ?> queryParams, Class<T> responseType,
            HttpClientCallback<T> callback) {
        executeWithCallback(callback, () -> get(url, queryParams, responseType));
    }

    @Override
    public <T> void get(String url, Map<String, String> headers, Map<String, ?> queryParams, Class<T> responseType,
            HttpClientCallback<T> callback) {
        executeWithCallback(callback, () -> get(url, headers, queryParams, responseType));
    }

    @Override
    public <T> void postJson(String url, Object requestBody, Class<T> responseType, HttpClientCallback<T> callback) {
        executeWithCallback(callback, () -> postJson(url, requestBody, responseType));
    }

    @Override
    public <T> void postJson(String url, Map<String, String> headers, Object requestBody, Class<T> responseType,
            HttpClientCallback<T> callback) {
        executeWithCallback(callback, () -> postJson(url, headers, requestBody, responseType));
    }

    @Override
    public <T> void postForm(String url, MultiValueMap<String, String> formData, Class<T> responseType,
            HttpClientCallback<T> callback) {
        executeWithCallback(callback, () -> postForm(url, formData, responseType));
    }

    @Override
    public <T> void postForm(String url, Map<String, String> headers, MultiValueMap<String, String> formData,
            Class<T> responseType, HttpClientCallback<T> callback) {
        executeWithCallback(callback, () -> postForm(url, headers, formData, responseType));
    }

    private <T> T execute(HttpMethod method, String url, MediaType contentType, Object requestBody,
            Map<String, String> customHeaders, Class<T> responseType, Object... uriVariables) {
        long start = System.currentTimeMillis();
        String traceId = TraceIdUtils.getTraceId();

        // 先把默认请求头和调用方请求头合并成可变上下文，后续拦截器可以继续修改。
        HttpClientRequestContext requestContext = new HttpClientRequestContext(
                method.name(), url, buildHeaders(traceId, contentType, customHeaders), requestBody);

        applyBeforeRequest(requestContext);

        // 拦截器可能修改请求体，因此日志内容必须在拦截器执行之后生成。
        String requestLogBody = toLogBody(requestContext.getRequestBody());
        logBegin(method, requestContext.getUrl(), traceId, requestLogBody);
        try {
            // 没有路径变量时直接使用 URI，避免 RestClient 重新解析已经编码好的 query string。
            RequestBodySpec requestSpec = (uriVariables == null || uriVariables.length == 0
                    ? restClient.method(method).uri(URI.create(requestContext.getUrl()))
                    : restClient.method(method).uri(requestContext.getUrl(), uriVariables))
                    .headers(headers -> requestContext.getHeaders().forEach((name, value) -> {
                        if (name != null && !name.isBlank() && value != null) {
                            headers.set(name, value);
                        }
                    }));
            if (requestContext.getRequestBody() != null) {
                requestSpec.body(requestContext.getRequestBody());
            }

            // exchange 可以读取原始响应体，便于统一处理非 2xx、日志和自定义反序列化。
            ClientResult<T> result = requestSpec.exchange((request, clientResponse) -> {
                        byte[] responseBytes = StreamUtils.copyToByteArray(clientResponse.getBody());
                        String responseBody = new String(responseBytes, StandardCharsets.UTF_8);
                        int statusCode = clientResponse.getStatusCode().value();
                        if (!clientResponse.getStatusCode().is2xxSuccessful()) {
                            throw new HttpClientException("External HTTP status error",
                                    method.name(), requestContext.getUrl(), statusCode, responseBody,
                                    HttpClientErrorType.HTTP_STATUS_ERROR, null);
                        }
                        applyAfterResponse(new HttpClientResponseContext(
                                method.name(), requestContext.getUrl(), statusCode, responseBody));
                        T body = readResponse(method, requestContext.getUrl(), statusCode, responseBytes,
                                responseBody, responseType);
                        return new ClientResult<>(body, statusCode, responseBody);
                    });
            logEnd(method, requestContext.getUrl(), traceId, result.statusCode(), result.responseBody(),
                    System.currentTimeMillis() - start, null);
            return result.body();
        } catch (HttpClientException exception) {
            applyOnError(new HttpClientErrorContext(method.name(), requestContext.getUrl(), exception));
            logEnd(method, requestContext.getUrl(), traceId, exception.getStatusCode(), exception.getResponseBody(),
                    System.currentTimeMillis() - start, exception);
            throw exception;
        } catch (ResourceAccessException exception) {
            HttpClientException wrapped = wrapAccessException(method, requestContext.getUrl(), exception);
            applyOnError(new HttpClientErrorContext(method.name(), requestContext.getUrl(), wrapped));
            logEnd(method, requestContext.getUrl(), traceId, null, null, System.currentTimeMillis() - start, wrapped);
            throw wrapped;
        } catch (RestClientException exception) {
            // exchange 中抛出的 HttpClientException 可能被 RestClientException 包装，这里拆出来保持错误类型准确。
            if (exception.getCause() instanceof HttpClientException httpClientException) {
                applyOnError(new HttpClientErrorContext(method.name(), requestContext.getUrl(), httpClientException));
                logEnd(method, requestContext.getUrl(), traceId, httpClientException.getStatusCode(),
                        httpClientException.getResponseBody(), System.currentTimeMillis() - start, httpClientException);
                throw httpClientException;
            }
            HttpClientException wrapped = new HttpClientException("External HTTP network error",
                    method.name(), requestContext.getUrl(), null, null, HttpClientErrorType.NETWORK_ERROR, exception);
            applyOnError(new HttpClientErrorContext(method.name(), requestContext.getUrl(), wrapped));
            logEnd(method, requestContext.getUrl(), traceId, null, null, System.currentTimeMillis() - start, wrapped);
            throw wrapped;
        }
    }

    private Map<String, String> buildHeaders(String traceId, MediaType contentType, Map<String, String> customHeaders) {
        Map<String, String> headers = new java.util.LinkedHashMap<>();
        headers.put(TraceIdUtils.TRACE_ID_HEADER, traceId);
        headers.put("User-Agent", properties.getUserAgent());
        headers.put("Accept", MediaType.APPLICATION_JSON_VALUE);
        if (contentType != null) {
            headers.put("Content-Type", contentType.toString());
        }
        if (customHeaders != null) {
            customHeaders.forEach((name, value) -> {
                if (name != null && !name.isBlank() && value != null) {
                    // 调用方自定义请求头优先级更高，可以覆盖同名默认请求头。
                    headers.put(name, value);
                }
            });
        }
        return headers;
    }

    private void applyBeforeRequest(HttpClientRequestContext context) {
        interceptors.forEach(interceptor -> interceptor.beforeRequest(context));
    }

    private void applyAfterResponse(HttpClientResponseContext context) {
        interceptors.forEach(interceptor -> interceptor.afterResponse(context));
    }

    private void applyOnError(HttpClientErrorContext context) {
        interceptors.forEach(interceptor -> interceptor.onError(context));
    }

    private <T> void executeWithCallback(HttpClientCallback<T> callback, HttpCall<T> call) {
        try {
            callback.onSuccess(call.execute());
        } catch (HttpClientException exception) {
            // 回调版本约定：HTTP 调用异常交给调用方的失败回调处理，不再继续向外抛出。
            callback.onFailure(exception);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T readResponse(HttpMethod method, String url, int statusCode, byte[] responseBytes,
            String responseBody, Class<T> responseType) {
        if (responseType == Void.class || responseType == Void.TYPE) {
            return null;
        }
        if (responseType == String.class) {
            return (T) responseBody;
        }
        if (responseType == byte[].class) {
            return (T) responseBytes;
        }
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(responseBody, responseType);
        } catch (IOException exception) {
            throw new HttpClientException("External HTTP response parse error",
                    method.name(), url, statusCode, responseBody,
                    HttpClientErrorType.RESPONSE_PARSE_ERROR, exception);
        }
    }

    private HttpClientException wrapAccessException(HttpMethod method, String url, ResourceAccessException exception) {
        HttpClientErrorType errorType = containsCause(exception, SocketTimeoutException.class)
                ? HttpClientErrorType.TIMEOUT
                : HttpClientErrorType.NETWORK_ERROR;
        return new HttpClientException("External HTTP access error",
                method.name(), url, null, null, errorType, exception);
    }

    private boolean containsCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String toLogBody(Object body) {
        if (body == null) {
            return "-";
        }
        if (body instanceof String string) {
            return string;
        }
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception ignored) {
            return String.valueOf(body);
        }
    }

    private void logBegin(HttpMethod method, String url, String traceId, String requestBody) {
        if (!properties.isLogEnabled()) {
            return;
        }
        log.info("HTTP OUT BEGIN {} {} traceId={}", method.name(), url, traceId);
        if (!"-".equals(requestBody)) {
            log.info("HTTP OUT REQ  {}", formatBody(requestBody));
        }
    }

    private void logEnd(HttpMethod method, String url, String traceId, Integer statusCode, String responseBody,
            long costMs, HttpClientException exception) {
        if (!properties.isLogEnabled()) {
            return;
        }
        if (exception == null) {
            log.info("HTTP OUT END   {} {} -> {} {}ms traceId={}", method.name(), url, statusCode, costMs, traceId);
            if (responseBody != null && !responseBody.isBlank()) {
                log.info("HTTP OUT RESP {}", formatBody(responseBody));
            }
            return;
        }
        log.warn("HTTP OUT END   {} {} -> {} {}ms traceId={} errorType={}",
                method.name(), url, statusCode == null ? "-" : statusCode, costMs, traceId, exception.getErrorType());
        if (responseBody != null && !responseBody.isBlank()) {
            log.warn("HTTP OUT RESP {}", formatBody(responseBody));
        }
    }

    private String formatBody(String body) {
        String masked = MaskUtils.maskJson(objectMapper, body);
        if (masked.length() <= properties.getMaxLogBodyLength()) {
            return masked;
        }
        return masked.substring(0, properties.getMaxLogBodyLength()) + "...";
    }

    @FunctionalInterface
    private interface HttpCall<T> {

        T execute();
    }

    private record ClientResult<T>(T body, int statusCode, String responseBody) {
    }
}
