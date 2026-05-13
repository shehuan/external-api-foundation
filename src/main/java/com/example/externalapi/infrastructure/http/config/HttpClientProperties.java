package com.example.externalapi.infrastructure.http.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * HTTP 客户端配置项。
 *
 * <p>配置前缀：{@code app.http-client}。</p>
 */
@ConfigurationProperties(prefix = "app.http-client")
public class HttpClientProperties {

    /**
     * 建立连接的超时时间，单位毫秒。
     */
    private int connectTimeoutMillis = 3000;

    /**
     * 等待读取响应的超时时间，单位毫秒。
     */
    private int readTimeoutMillis = 10000;

    /**
     * 默认 User-Agent。
     */
    private String userAgent = "external-api-foundation";

    /**
     * 是否打印出站 HTTP 日志。
     */
    private boolean logEnabled = true;

    /**
     * 请求/响应 body 日志最大长度，超过后截断。
     */
    private int maxLogBodyLength = 2000;

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    public int getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    public void setReadTimeoutMillis(int readTimeoutMillis) {
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public boolean isLogEnabled() {
        return logEnabled;
    }

    public void setLogEnabled(boolean logEnabled) {
        this.logEnabled = logEnabled;
    }

    public int getMaxLogBodyLength() {
        return maxLogBodyLength;
    }

    public void setMaxLogBodyLength(int maxLogBodyLength) {
        this.maxLogBodyLength = maxLogBodyLength;
    }
}
