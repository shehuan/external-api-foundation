package com.example.externalapi.infrastructure.crypto.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 接口加解密配置。
 *
 * <p>配置前缀：app.crypto。</p>
 *
 * <p>请求是否解密、响应是否加密分别配置，但算法和密钥协商元数据统一从 Header 获取。</p>
 * <p>这样可以同时兼容：请求密文 + 响应密文、请求明文 + 响应密文、请求密文 + 响应明文。</p>
 */
@ConfigurationProperties(prefix = "app.crypto")
public class CryptoProperties {

    /**
     * 总开关。false 时整个 CryptoFilter 不处理请求。
     */
    private boolean enabled = false;

    /**
     * 请求解密路径规则。
     */
    private PathRule requestDecrypt = new PathRule();

    /**
     * 响应加密路径规则。
     */
    private PathRule responseEncrypt = new PathRule();

    /**
     * 调用方配置。默认实现按 X-App-Id 在这里查找密钥。
     */
    private List<Client> clients = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public PathRule getRequestDecrypt() {
        return requestDecrypt;
    }

    public void setRequestDecrypt(PathRule requestDecrypt) {
        this.requestDecrypt = requestDecrypt;
    }

    public PathRule getResponseEncrypt() {
        return responseEncrypt;
    }

    public void setResponseEncrypt(PathRule responseEncrypt) {
        this.responseEncrypt = responseEncrypt;
    }

    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }

    public static class PathRule {

        /**
         * 子开关。用于只关闭请求解密或只关闭响应加密。
         */
        private boolean enabled = true;

        /**
         * 纳入处理的路径。
         */
        private List<String> includePaths = new ArrayList<>(List.of("/**"));

        /**
         * 排除路径。excludePaths 优先级高于 includePaths。
         */
        private List<String> excludePaths = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
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

    public static class Client {

        /**
         * 调用方应用标识，对应请求头 X-App-Id。
         */
        private String appId;

        /**
         * 共享密钥、私钥、KMS keyId 或其他密钥引用。
         */
        private String secret;

        /**
         * 当前调用方是否启用。
         */
        private boolean enabled = true;

        public String getAppId() {
            return appId;
        }

        public void setAppId(String appId) {
            this.appId = appId;
        }

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
