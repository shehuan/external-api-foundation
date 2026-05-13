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
 * 请求重放防护过滤器。
 *
 * <p>它解决的问题是：攻击者截获一次合法请求后，不能在有效期内反复发送同一份请求。</p>
 *
 * <p>核心校验项：</p>
 * <p>1. X-App-Id：识别调用方，并找到验签密钥材料。</p>
 * <p>2. X-Timestamp：限制请求时间窗口。</p>
 * <p>3. X-Nonce：一次性随机数，防止同一请求重复使用。</p>
 * <p>4. X-Sign：调用方对请求关键内容的签名，具体验签算法由服务端配置决定。</p>
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
        if (!requiresReplayProtection(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String appId = request.getHeader("X-App-Id");
        String timestamp = request.getHeader("X-Timestamp");
        String nonce = request.getHeader("X-Nonce");
        String sign = request.getHeader("X-Sign");

        // 缺少协议必填头时，后续链路无法再恢复这些信息。
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

        long requestTimeMillis;
        try {
            requestTimeMillis = Long.parseLong(timestamp);
        } catch (Exception exception) {
            JsonResponseWriter.write(response, objectMapper, ErrorCode.TIMESTAMP_INVALID);
            return;
        }

        long now = System.currentTimeMillis();
        long windowMillis = Duration.ofSeconds(properties.getTimestampWindowSeconds()).toMillis();
        if (Math.abs(now - requestTimeMillis) > windowMillis) {
            JsonResponseWriter.write(response, objectMapper, ErrorCode.REQUEST_EXPIRED);
            return;
        }

        Optional<ReplayProtectionProperties.ApiClient> client = findClient(appId);
        if (client.isEmpty()) {
            JsonResponseWriter.write(response, objectMapper, ErrorCode.APP_ID_INVALID);
            return;
        }

        String signingContent = buildSigningContent(request, timestamp, nonce, appId);

        // 算法选择由服务端配置决定，不能由客户端请求字段控制。
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

        // nonce 保存时间略长于时间戳窗口，用于吸收客户端和服务端的小幅时钟差。
        Duration ttl = Duration.ofSeconds(properties.getTimestampWindowSeconds() + 60);
        if (!nonceStore.markIfAbsent(appId, nonce, ttl)) {
            JsonResponseWriter.write(response, objectMapper, ErrorCode.REPLAY_REQUEST);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean requiresReplayProtection(HttpServletRequest request) {
        if (!properties.isEnabled() || PathMatchUtils.isOptions(request)) {
            return false;
        }
        String path = request.getRequestURI();
        if (PathMatchUtils.matchesAny(properties.getExcludePaths(), path)) {
            return false;
        }
        return PathMatchUtils.matchesAny(properties.getIncludePaths(), path);
    }

    private Optional<ReplayProtectionProperties.ApiClient> findClient(String appId) {
        return properties.getClients().stream()
                .filter(ReplayProtectionProperties.ApiClient::isEnabled)
                .filter(client -> appId.equals(client.getAppId()))
                .findFirst();
    }

    private String buildSigningContent(HttpServletRequest request, String timestamp, String nonce, String appId) {
        // 请求体参与签名时使用摘要，避免直接拼接大 body。
        byte[] body = request instanceof CachedBodyHttpServletRequest cachedRequest
                ? cachedRequest.getCachedBody()
                : new byte[0];

        // 签名原文必须稳定、明确，并且客户端和服务端保持一致。
        // 每行一个字段，避免随意字符串拼接带来的边界歧义。
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
        // Query 参数规范化排序，保证等价请求生成相同签名。
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
