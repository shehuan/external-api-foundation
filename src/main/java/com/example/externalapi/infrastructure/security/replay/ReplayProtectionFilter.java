package com.example.externalapi.infrastructure.security.replay;

import com.example.externalapi.common.error.ErrorCode;
import com.example.externalapi.infrastructure.crypto.web.CryptoFilter;
import com.example.externalapi.infrastructure.logging.CachedBodyHttpServletRequest;
import com.example.externalapi.infrastructure.web.JsonResponseWriter;
import com.example.externalapi.infrastructure.web.PathMatchUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 请求重放防护过滤器�? *
 * <p>它解决的问题是：攻击者截获一次合法请求后，不能在有效期内反复发送同一份请求�?/p>
 * <p>核心校验项：</p>
 * <p>1. X-App-Id：识别调用方，并找到验签密钥材料�?/p>
 * <p>2. X-Timestamp：限制请求时间窗口�?/p>
 * <p>3. X-Nonce：一次性随机数，防止同一请求重复使用�?/p>
 * <p>4. X-Sign：调用方对请求关键内容的签名，具体验签算法由服务端配置决定�?/p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@Component
public class ReplayProtectionFilter extends OncePerRequestFilter {

    private final ReplayProtectionProperties properties;
    private final RedisNonceStore nonceStore;
    private final ObjectMapper objectMapper;
    private final RequestSignatureVerifierRegistry verifierRegistry;

    public ReplayProtectionFilter(ReplayProtectionProperties properties, RedisNonceStore nonceStore,
            ObjectMapper objectMapper, RequestSignatureVerifierRegistry verifierRegistry) {
        this.properties = properties;
        this.nonceStore = nonceStore;
        this.objectMapper = objectMapper;
        this.verifierRegistry = verifierRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 第一步：根据开关和路径规则判断当前请求是否需要重放防护�?
        if (!requiresReplayProtection(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 第二步：读取重放防护相关请求头�?
        String appId = request.getHeader("X-App-Id");
        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String sign = request.getHeader("X-Sign");

        // 第三步：校验必填头。缺少任何关键字段都不能继续�?
        if (appId == null || appId.isBlank()) {
            JsonResponseWriter.write(response, objectMapper, ErrorCode.APP_ID_INVALID);
            return;
        }
        if (nonce == null || nonce.isBlank()) {
            JsonResponseWriter.write(response, objectMapper, ErrorCode.NONCE_MISSING);
            return;
        }
        if (sign == null || sign.isBlank()) {
            JsonResponseWriter.write(response, objectMapper, ErrorCode.SIGN_MISSING);
            return;
        }

        // 第四步：解析并校验时间戳格式�?
        long requestTimeMillis;
        try {
            requestTimeMillis = Long.parseLong(timestamp);
        } catch (Exception exception) {
            JsonResponseWriter.write(response, objectMapper, ErrorCode.TIMESTAMP_INVALID);
            return;
        }

        // 第五步：校验请求时间窗口。超过允许窗口说明请求过旧或客户端时间异常�?
        long now = System.currentTimeMillis();
        long windowMillis = Duration.ofSeconds(properties.getTimestampWindowSeconds()).toMillis();
        if (Math.abs(now - requestTimeMillis) > windowMillis) {
            JsonResponseWriter.write(response, objectMapper, ErrorCode.REQUEST_EXPIRED);
            return;
        }

        // 第六步：根据 appId 找到调用方验签密钥材料�?
        Optional<ReplayProtectionProperties.ApiClient> client = findClient(appId);
        if (client.isEmpty()) {
            JsonResponseWriter.write(response, objectMapper, ErrorCode.APP_ID_INVALID);
            return;
        }

        // 第七步：服务端按固定规则构造签名原文�?
        String signingContent = buildSigningContent(request, timestamp, nonce, appId);

        // 第八步：根据服务端配置选择验签算法。调用方不传算法，避免算法被请求参数影响。
        RequestSignatureVerifier verifier = verifierRegistry.getVerifier(properties.getSignatureAlgorithm());
        SignatureVerifyContext verifyContext = new SignatureVerifyContext(
                appId,
                client.get().getSignatureKey(),
                sign,
                signingContent,
                request);
        if (!verifier.verify(verifyContext)) {
            JsonResponseWriter.write(response, objectMapper, ErrorCode.SIGN_INVALID);
            return;
        }

        // 第九步：�?nonce 写入 Redis。SET NX 成功表示第一次使用，失败表示 nonce 已经被用过�?
        Duration ttl = Duration.ofSeconds(properties.getTimestampWindowSeconds() + 60);
        if (!nonceStore.markIfAbsent(appId, nonce, ttl)) {
            JsonResponseWriter.write(response, objectMapper, ErrorCode.REPLAY_REQUEST);
            return;
        }

        // 第十步：所有校验通过，继续后�?Filter�?
        filterChain.doFilter(request, response);
    }

    private boolean requiresReplayProtection(HttpServletRequest request) {
        // 总开关关闭或 OPTIONS 预检请求，不做重放防护�?
        if (!properties.isEnabled() || PathMatchUtils.isOptions(request)) {
            return false;
        }
        String path = request.getRequestURI();
        // exclude 优先级高�?include�?
        if (PathMatchUtils.matchesAny(properties.getExcludePaths(), path)) {
            return false;
        }
        return PathMatchUtils.matchesAny(properties.getIncludePaths(), path);
    }

    private Optional<ReplayProtectionProperties.ApiClient> findClient(String appId) {
        // 只允许启用状态的调用方参与签名校验�?
        return properties.getClients().stream()
                .filter(ReplayProtectionProperties.ApiClient::isEnabled)
                .filter(client -> appId.equals(client.getAppId()))
                .findFirst();
    }

    private String buildSigningContent(HttpServletRequest request, String timestamp, String nonce, String appId) {
        // 请求体参与签名时使用 SHA-256 摘要，避免直接拼接大 body。
        byte[] body = request instanceof CachedBodyHttpServletRequest cachedRequest
                ? cachedRequest.getCachedBody()
                : new byte[0];

        // 签名原文必须稳定、明确、双方一致。
        // 每一行一个字段，避免字符串拼接歧义。
        return String.join("\n",
                request.getMethod().toUpperCase(),
                request.getRequestURI(),
                canonicalQuery(request),
                SignatureUtils.sha256Hex(body),
                timestamp,
                nonce,
                appId,
                header(request, CryptoFilter.CRYPTO_ALGORITHM_HEADER),
                header(request, CryptoFilter.CRYPTO_ENCRYPTED_KEY_HEADER),
                header(request, CryptoFilter.CRYPTO_IV_HEADER));
    }

    private String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null ? "" : value;
    }

    private String canonicalQuery(HttpServletRequest request) {
        // Query 参数需要规范化排序，避免同样参数不同顺序导致签名不一致�?
        Map<String, String[]> parameterMap = request.getParameterMap();
        if (parameterMap.isEmpty()) {
            return "";
        }
        return parameterMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> java.util.Arrays.stream(entry.getValue())
                        .sorted(Comparator.naturalOrder())
                        .map(value -> entry.getKey() + "=" + value)
                        .collect(Collectors.joining("&")))
                .collect(Collectors.joining("&"));
    }
}
