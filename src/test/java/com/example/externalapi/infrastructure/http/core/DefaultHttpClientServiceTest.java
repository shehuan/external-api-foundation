package com.example.externalapi.infrastructure.http.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.example.externalapi.infrastructure.http.api.HttpClientCallbackAdapter;
import com.example.externalapi.infrastructure.http.api.HttpClientService;
import com.example.externalapi.infrastructure.http.config.HttpClientProperties;
import com.example.externalapi.infrastructure.http.exception.HttpClientErrorType;
import com.example.externalapi.infrastructure.http.exception.HttpClientException;
import com.example.externalapi.infrastructure.http.interceptor.HttpClientErrorContext;
import com.example.externalapi.infrastructure.http.interceptor.HttpClientInterceptor;
import com.example.externalapi.infrastructure.http.interceptor.HttpClientRequestContext;
import com.example.externalapi.infrastructure.http.interceptor.HttpClientResponseContext;
import com.example.externalapi.infrastructure.http.support.HttpHeadersBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * HTTP 客户端单元测试。
 *
 * <p>使用 MockRestServiceServer 模拟外部 HTTP 服务，不依赖真实网络端口，测试更稳定。</p>
 * <p>覆盖 GET、POST JSON、POST FORM、自定义请求头、回调、异常和拦截器扩展点。</p>
 */
class DefaultHttpClientServiceTest {

    private static final String BASE_URL = "https://example.com";

    private MockRestServiceServer mockServer;
    private HttpClientService httpClientService;

    @BeforeEach
    void setUp() {
        // 每个测试使用独立的 RestClient.Builder 和 MockRestServiceServer，避免请求期望互相污染。
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        httpClientService = new DefaultHttpClientService(builder.build(), new ObjectMapper(), properties(), List.of());
    }

    @Test
    void getShouldSupportPathVariables() {
        mockServer.expect(once(), requestTo(BASE_URL + "/echo/users/10001"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("GET /echo/users/10001", MediaType.TEXT_PLAIN));

        String response = httpClientService.get(BASE_URL + "/echo/users/{id}", String.class, 10001L);

        assertEquals("GET /echo/users/10001", response);
        mockServer.verify();
    }

    @Test
    void getShouldSupportQueryParams() {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("userId", 10001L);
        query.put("status", "PAID");
        query.put("keyword", "hello world");

        mockServer.expect(once(), requestTo(BASE_URL + "/echo/orders?userId=10001&status=PAID&keyword=hello%20world"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("query-ok", MediaType.TEXT_PLAIN));

        String response = httpClientService.get(BASE_URL + "/echo/orders", query, String.class);

        assertEquals("query-ok", response);
        mockServer.verify();
    }

    @Test
    void postJsonShouldSendJsonBody() {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("userId", 10001L);
        request.put("status", "PAID");

        mockServer.expect(once(), requestTo(BASE_URL + "/body"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "application/json"))
                .andExpect(content().json("""
                        {
                          "userId": 10001,
                          "status": "PAID"
                        }
                        """))
                .andRespond(withSuccess("json-ok", MediaType.TEXT_PLAIN));

        String response = httpClientService.postJson(BASE_URL + "/body", request, String.class);

        assertEquals("json-ok", response);
        mockServer.verify();
    }

    @Test
    void postJsonShouldSupportCustomHeaders() {
        Map<String, String> headers = HttpHeadersBuilder.create()
                .bearerToken("abc123")
                .appId("partner-a")
                .build();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("userId", 10001L);

        mockServer.expect(once(), requestTo(BASE_URL + "/json/headers"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer abc123"))
                .andExpect(header("X-App-Id", "partner-a"))
                .andExpect(header("Content-Type", "application/json"))
                .andExpect(content().json("""
                        {
                          "userId": 10001
                        }
                        """))
                .andRespond(withSuccess("json-headers-ok", MediaType.TEXT_PLAIN));

        String response = httpClientService.postJson(BASE_URL + "/json/headers", headers, request, String.class);

        assertEquals("json-headers-ok", response);
        mockServer.verify();
    }

    @Test
    void postFormShouldSendFormBody() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", "admin");
        form.add("password", "123456");

        mockServer.expect(once(), requestTo(BASE_URL + "/body"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
                .andExpect(content().string("username=admin&password=123456"))
                .andRespond(withSuccess("form-ok", MediaType.TEXT_PLAIN));

        String response = httpClientService.postForm(BASE_URL + "/body", form, String.class);

        assertEquals("form-ok", response);
        mockServer.verify();
    }

    @Test
    void postFormShouldSupportCustomHeaders() {
        Map<String, String> headers = HttpHeadersBuilder.create()
                .bearerToken("abc123")
                .appId("partner-a")
                .build();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", "admin");

        mockServer.expect(once(), requestTo(BASE_URL + "/form/headers"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer abc123"))
                .andExpect(header("X-App-Id", "partner-a"))
                .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
                .andExpect(content().string("username=admin"))
                .andRespond(withSuccess("form-headers-ok", MediaType.TEXT_PLAIN));

        String response = httpClientService.postForm(BASE_URL + "/form/headers", headers, form, String.class);

        assertEquals("form-headers-ok", response);
        mockServer.verify();
    }

    @Test
    void getWithQueryParamsShouldSupportCustomHeaders() {
        Map<String, String> headers = HttpHeadersBuilder.create()
                .bearerToken("abc123")
                .appId("partner-a")
                .header("X-Custom", "custom-value")
                .build();

        mockServer.expect(once(), requestTo(BASE_URL + "/headers"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer abc123"))
                .andExpect(header("X-App-Id", "partner-a"))
                .andExpect(header("X-Custom", "custom-value"))
                .andRespond(withSuccess("headers-ok", MediaType.TEXT_PLAIN));

        String response = httpClientService.get(BASE_URL + "/headers", headers, Map.of(), String.class);

        assertEquals("headers-ok", response);
        mockServer.verify();
    }

    @Test
    void getWithPathVariablesShouldSupportCustomHeaders() {
        Map<String, String> headers = HttpHeadersBuilder.create()
                .bearerToken("abc123")
                .appId("partner-a")
                .build();

        mockServer.expect(once(), requestTo(BASE_URL + "/headers/10001"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer abc123"))
                .andExpect(header("X-App-Id", "partner-a"))
                .andRespond(withSuccess("path-headers-ok", MediaType.TEXT_PLAIN));

        String response = httpClientService.get(BASE_URL + "/headers/{id}", headers, String.class, 10001L);

        assertEquals("path-headers-ok", response);
        mockServer.verify();
    }

    @Test
    void shouldThrowHttpClientExceptionWhenStatusIsNot2xx() {
        mockServer.expect(once(), requestTo(BASE_URL + "/error"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError().body("service unavailable").contentType(MediaType.TEXT_PLAIN));

        HttpClientException exception = assertThrows(HttpClientException.class,
                () -> httpClientService.get(BASE_URL + "/error", String.class));

        assertEquals(500, exception.getStatusCode());
        assertEquals("service unavailable", exception.getResponseBody());
        assertEquals(HttpClientErrorType.HTTP_STATUS_ERROR, exception.getErrorType());
        mockServer.verify();
    }

    @Test
    void callbackAdapterShouldHandleSuccess() {
        // 回调方法没有返回值，用 AtomicReference 捕获回调结果便于断言。
        AtomicReference<String> success = new AtomicReference<>();
        AtomicReference<HttpClientException> failure = new AtomicReference<>();

        mockServer.expect(once(), requestTo(BASE_URL + "/echo/callback"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("callback-ok", MediaType.TEXT_PLAIN));

        httpClientService.get(
                BASE_URL + "/echo/callback",
                String.class,
                HttpClientCallbackAdapter.of(success::set, failure::set));

        assertEquals("callback-ok", success.get());
        assertEquals(null, failure.get());
        mockServer.verify();
    }

    @Test
    void callbackGetWithHeadersShouldHandleSuccess() {
        AtomicReference<String> success = new AtomicReference<>();
        AtomicReference<HttpClientException> failure = new AtomicReference<>();
        Map<String, String> headers = HttpHeadersBuilder.create()
                .bearerToken("abc123")
                .build();

        mockServer.expect(once(), requestTo(BASE_URL + "/echo/callback/10001"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer abc123"))
                .andRespond(withSuccess("callback-path-headers-ok", MediaType.TEXT_PLAIN));

        httpClientService.get(
                BASE_URL + "/echo/callback/{id}",
                headers,
                String.class,
                HttpClientCallbackAdapter.of(success::set, failure::set),
                10001L);

        assertEquals("callback-path-headers-ok", success.get());
        assertEquals(null, failure.get());
        mockServer.verify();
    }

    @Test
    void callbackGetWithQueryParamsShouldHandleSuccess() {
        AtomicReference<String> success = new AtomicReference<>();
        AtomicReference<HttpClientException> failure = new AtomicReference<>();
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("status", "PAID");

        mockServer.expect(once(), requestTo(BASE_URL + "/echo/callback/query?status=PAID"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("callback-query-ok", MediaType.TEXT_PLAIN));

        httpClientService.get(
                BASE_URL + "/echo/callback/query",
                query,
                String.class,
                HttpClientCallbackAdapter.of(success::set, failure::set));

        assertEquals("callback-query-ok", success.get());
        assertEquals(null, failure.get());
        mockServer.verify();
    }

    @Test
    void callbackGetWithHeadersAndQueryParamsShouldHandleSuccess() {
        AtomicReference<String> success = new AtomicReference<>();
        AtomicReference<HttpClientException> failure = new AtomicReference<>();
        Map<String, String> headers = HttpHeadersBuilder.create()
                .appId("partner-a")
                .build();
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("status", "PAID");

        mockServer.expect(once(), requestTo(BASE_URL + "/echo/callback/query-headers?status=PAID"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-App-Id", "partner-a"))
                .andRespond(withSuccess("callback-query-headers-ok", MediaType.TEXT_PLAIN));

        httpClientService.get(
                BASE_URL + "/echo/callback/query-headers",
                headers,
                query,
                String.class,
                HttpClientCallbackAdapter.of(success::set, failure::set));

        assertEquals("callback-query-headers-ok", success.get());
        assertEquals(null, failure.get());
        mockServer.verify();
    }

    @Test
    void callbackPostJsonShouldHandleSuccess() {
        AtomicReference<String> success = new AtomicReference<>();
        AtomicReference<HttpClientException> failure = new AtomicReference<>();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("userId", 10001L);

        mockServer.expect(once(), requestTo(BASE_URL + "/callback/json"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "application/json"))
                .andExpect(content().json("""
                        {
                          "userId": 10001
                        }
                        """))
                .andRespond(withSuccess("callback-json-ok", MediaType.TEXT_PLAIN));

        httpClientService.postJson(
                BASE_URL + "/callback/json",
                request,
                String.class,
                HttpClientCallbackAdapter.of(success::set, failure::set));

        assertEquals("callback-json-ok", success.get());
        assertEquals(null, failure.get());
        mockServer.verify();
    }

    @Test
    void callbackPostJsonWithHeadersShouldHandleSuccess() {
        AtomicReference<String> success = new AtomicReference<>();
        AtomicReference<HttpClientException> failure = new AtomicReference<>();
        Map<String, String> headers = HttpHeadersBuilder.create()
                .appId("partner-a")
                .build();
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("userId", 10001L);

        mockServer.expect(once(), requestTo(BASE_URL + "/callback/json/headers"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-App-Id", "partner-a"))
                .andExpect(header("Content-Type", "application/json"))
                .andExpect(content().json("""
                        {
                          "userId": 10001
                        }
                        """))
                .andRespond(withSuccess("callback-json-headers-ok", MediaType.TEXT_PLAIN));

        httpClientService.postJson(
                BASE_URL + "/callback/json/headers",
                headers,
                request,
                String.class,
                HttpClientCallbackAdapter.of(success::set, failure::set));

        assertEquals("callback-json-headers-ok", success.get());
        assertEquals(null, failure.get());
        mockServer.verify();
    }

    @Test
    void callbackPostFormShouldHandleSuccess() {
        AtomicReference<String> success = new AtomicReference<>();
        AtomicReference<HttpClientException> failure = new AtomicReference<>();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", "admin");

        mockServer.expect(once(), requestTo(BASE_URL + "/callback/form"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
                .andExpect(content().string("username=admin"))
                .andRespond(withSuccess("callback-form-ok", MediaType.TEXT_PLAIN));

        httpClientService.postForm(
                BASE_URL + "/callback/form",
                form,
                String.class,
                HttpClientCallbackAdapter.of(success::set, failure::set));

        assertEquals("callback-form-ok", success.get());
        assertEquals(null, failure.get());
        mockServer.verify();
    }

    @Test
    void callbackPostFormWithHeadersShouldHandleSuccess() {
        AtomicReference<String> success = new AtomicReference<>();
        AtomicReference<HttpClientException> failure = new AtomicReference<>();
        Map<String, String> headers = HttpHeadersBuilder.create()
                .appId("partner-a")
                .build();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", "admin");

        mockServer.expect(once(), requestTo(BASE_URL + "/callback/form/headers"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("X-App-Id", "partner-a"))
                .andExpect(header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8"))
                .andExpect(content().string("username=admin"))
                .andRespond(withSuccess("callback-form-headers-ok", MediaType.TEXT_PLAIN));

        httpClientService.postForm(
                BASE_URL + "/callback/form/headers",
                headers,
                form,
                String.class,
                HttpClientCallbackAdapter.of(success::set, failure::set));

        assertEquals("callback-form-headers-ok", success.get());
        assertEquals(null, failure.get());
        mockServer.verify();
    }

    @Test
    void callbackAdapterShouldHandleFailureWithoutThrowingHttpClientException() {
        // 回调版本约定：HTTP 调用失败进入 failure 回调，不再向测试方法抛出 HttpClientException。
        AtomicReference<String> success = new AtomicReference<>();
        AtomicReference<HttpClientException> failure = new AtomicReference<>();

        mockServer.expect(once(), requestTo(BASE_URL + "/error"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError().body("service unavailable").contentType(MediaType.TEXT_PLAIN));

        httpClientService.get(
                BASE_URL + "/error",
                String.class,
                HttpClientCallbackAdapter.of(success::set, failure::set));

        assertEquals(null, success.get());
        assertEquals(500, failure.get().getStatusCode());
        assertEquals(HttpClientErrorType.HTTP_STATUS_ERROR, failure.get().getErrorType());
        mockServer.verify();
    }

    @Test
    void interceptorShouldModifyRequestHeaders() {
        HttpClientService service = new DefaultHttpClientService(
                mockServerRestClient(),
                new ObjectMapper(),
                properties(),
                List.of(new HttpClientInterceptor() {
                    @Override
                    public void beforeRequest(HttpClientRequestContext context) {
                        context.getHeaders().put("X-Custom", "from-interceptor");
                    }
                }));

        mockServer.expect(once(), requestTo(BASE_URL + "/headers"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("X-Custom", "from-interceptor"))
                .andRespond(withSuccess("interceptor-ok", MediaType.TEXT_PLAIN));

        String response = service.get(BASE_URL + "/headers", String.class);

        assertEquals("interceptor-ok", response);
        mockServer.verify();
    }

    @Test
    void interceptorShouldObserveResponse() {
        AtomicReference<String> responseBody = new AtomicReference<>();
        HttpClientService service = new DefaultHttpClientService(
                mockServerRestClient(),
                new ObjectMapper(),
                properties(),
                List.of(new HttpClientInterceptor() {
                    @Override
                    public void afterResponse(HttpClientResponseContext context) {
                        responseBody.set(context.getResponseBody());
                    }
                }));

        mockServer.expect(once(), requestTo(BASE_URL + "/echo/interceptor"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("GET /echo/interceptor", MediaType.TEXT_PLAIN));

        service.get(BASE_URL + "/echo/interceptor", String.class);

        assertEquals("GET /echo/interceptor", responseBody.get());
        mockServer.verify();
    }

    @Test
    void interceptorShouldObserveError() {
        AtomicReference<HttpClientErrorType> errorType = new AtomicReference<>();
        HttpClientService service = new DefaultHttpClientService(
                mockServerRestClient(),
                new ObjectMapper(),
                properties(),
                List.of(new HttpClientInterceptor() {
                    @Override
                    public void onError(HttpClientErrorContext context) {
                        errorType.set(context.getException().getErrorType());
                    }
                }));

        mockServer.expect(once(), requestTo(BASE_URL + "/error"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError().body("service unavailable").contentType(MediaType.TEXT_PLAIN));

        assertThrows(HttpClientException.class, () -> service.get(BASE_URL + "/error", String.class));

        assertEquals(HttpClientErrorType.HTTP_STATUS_ERROR, errorType.get());
        mockServer.verify();
    }

    private RestClient mockServerRestClient() {
        // 拦截器测试需要重新创建 service，因此同步重建 mockServer 和 RestClient。
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        return builder.build();
    }

    private HttpClientProperties properties() {
        HttpClientProperties properties = new HttpClientProperties();
        properties.setLogEnabled(false);
        return properties;
    }
}
