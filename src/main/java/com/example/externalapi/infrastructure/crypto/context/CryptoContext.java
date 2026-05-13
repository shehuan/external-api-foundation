package com.example.externalapi.infrastructure.crypto.context;

import java.util.HashMap;
import java.util.Map;

/**
 * 加解密请求上下文�? *
 * <p>这个对象只在一�?HTTP 请求内有效，用于把“请求解密阶段”的信息传递给“响应加密阶段”�?/p>
 *
 * <p>典型用途：</p>
 * <p>1. 保存 appId、请求路径、traceId 等基础信息�?/p>
 * <p>2. 混合加密时，算法实现可以把本次请求解析出的临时对称密钥放�?attributes�?/p>
 * <p>3. 响应加密时，算法实现再从 attributes 取出同一个临时对称密钥加密响应�?/p>
 */
public class CryptoContext {

    private String appId;

    private String method;

    private String path;

    private String traceId;

    /**
     * 算法扩展参数�?     *
     * <p>框架本身不会解析这里面的值，只负责在同一次请求内保存和传递�?/p>
     * <p>例如：RSA + AES 混合加密时，可以把解密后�?AES key 放到这里�?/p>
     */
    private final Map<String, Object> attributes = new HashMap<>();

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }
}
