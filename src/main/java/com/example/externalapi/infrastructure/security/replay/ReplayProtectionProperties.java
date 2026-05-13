package com.example.externalapi.infrastructure.security.replay;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 请求重放防护配置�? *
 * <p>配置前缀：app.security.replay�?/p>
 */
@ConfigurationProperties(prefix = "app.security.replay")
public class ReplayProtectionProperties {

    /**
     * 请求重放防护总开关�?     */
    private boolean enabled = false;

    /**
     * 允许的时间戳偏移窗口，单位秒�?     */
    private long timestampWindowSeconds = 300;

    /**
     * 服务端约定的验签算法。调用方不需要通过请求头传算法�?     */
    private String signatureAlgorithm = "HMAC_SHA256";

    /**
     * 需要启用重放防护的路径�?     */
    private List<String> includePaths = new ArrayList<>();

    /**
     * 不启用重放防护的路径，优先级高于 includePaths�?     */
    private List<String> excludePaths = new ArrayList<>();

    /**
     * 外部调用方配置，用于通过 appId 查找验签密钥材料�?     */
    private List<ApiClient> clients = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getTimestampWindowSeconds() {
        return timestampWindowSeconds;
    }

    public void setTimestampWindowSeconds(long timestampWindowSeconds) {
        this.timestampWindowSeconds = timestampWindowSeconds;
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
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

    public List<ApiClient> getClients() {
        return clients;
    }

    public void setClients(List<ApiClient> clients) {
        this.clients = clients;
    }

    public static class ApiClient {
        /**
         * 调用方应用标识，对应请求�?X-App-Id�?         */
        private String appId;

        /**
         * 验签密钥材料或密钥引用�?         *
         * <p>HMAC 场景下表示共享密钥；RSA/SM2 场景下可以表示调用方公钥�?         * KMS/证书场景下可以表�?keyId 或证�?ID�?/p>
         */
        private String signatureKey;

        /**
         * 当前调用方是否启用�?         */
        private boolean enabled = true;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getSignatureKey() {
            return signatureKey;
        }

        public void setSignatureKey(String signatureKey) {
            this.signatureKey = signatureKey;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
