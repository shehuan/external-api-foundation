package com.example.externalapi.infrastructure.security.jwt;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置。
 *
 * <p>配置前缀：app.jwt。</p>
 * <p>includePaths 表示需要鉴权的路径，excludePaths 表示跳过鉴权的路径；exclude 优先。</p>
 */
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /**
     * JWT 鉴权总开关。
     */
    private boolean enabled = true;

    /**
     * token 签发者。解析 token 时会校验 issuer，避免其他系统签发的 token 被误用。
     */
    private String issuer;

    /**
     * Base64 编码后的 HMAC 签名密钥。
     */
    private String secret;

    /**
     * 访问 token 过期时间，单位分钟。
     */
    private long accessTokenExpireMinutes = 120;

    /**
     * 需要 JWT 鉴权的路径。
     */
    private List<String> includePaths = new ArrayList<>();

    /**
     * 不需要 JWT 鉴权的路径。
     */
    private List<String> excludePaths = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getAccessTokenExpireMinutes() {
        return accessTokenExpireMinutes;
    }

    public void setAccessTokenExpireMinutes(long accessTokenExpireMinutes) {
        this.accessTokenExpireMinutes = accessTokenExpireMinutes;
    }

    public List<String> getIncludePaths() {
        return includePaths;
    }

    public void setIncludePaths(List<String> includePaths) {
        this.includePaths = includePaths;
    }

    public List<String> getExcludePaths() {
        return excludePaths;
    }

    public void setExcludePaths(List<String> excludePaths) {
        this.excludePaths = excludePaths;
    }
}
