package com.example.externalapi.infrastructure.http.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * HTTP 客户端基础配置。
 *
 * <p>这里只创建底层 {@link RestClient} Bean。业务侧统一依赖
 * {@code HttpClientService}，不直接注入 {@link RestClient}。</p>
 */
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient restClient(HttpClientProperties properties) {
        // 第一版使用 JDK HttpURLConnection；后续需要连接池时可替换为 Apache HttpClient requestFactory。
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMillis());
        requestFactory.setReadTimeout(properties.getReadTimeoutMillis());
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
