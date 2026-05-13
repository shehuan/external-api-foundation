package com.example.externalapi.infrastructure.crypto.web;

import com.example.externalapi.common.error.ErrorCode;
import com.example.externalapi.common.exception.BizException;
import com.example.externalapi.infrastructure.crypto.config.CryptoProperties;
import com.example.externalapi.infrastructure.crypto.context.CryptoContext;
import com.example.externalapi.infrastructure.crypto.key.CryptoKeyResolver;
import com.example.externalapi.infrastructure.crypto.model.CryptoEnvelope;
import com.example.externalapi.infrastructure.crypto.model.CryptoKey;
import com.example.externalapi.infrastructure.crypto.model.CryptoMetadata;
import com.example.externalapi.infrastructure.crypto.provider.PayloadCryptoProvider;
import com.example.externalapi.infrastructure.crypto.provider.PayloadCryptoProviderRegistry;
import com.example.externalapi.infrastructure.logging.CachedBodyHttpServletRequest;
import com.example.externalapi.infrastructure.logging.RequestLogAttributes;
import com.example.externalapi.infrastructure.logging.TraceIdUtils;
import com.example.externalapi.infrastructure.web.JsonResponseWriter;
import com.example.externalapi.infrastructure.web.PathMatchUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * 请求解密、响应加密过滤器。
 *
 * <p>安全协议元数据统一放在 Header 中：
 * X-App-Id / X-Crypto-Algorithm / X-Crypto-Encrypted-Key / X-Crypto-IV。</p>
 *
 * <p>请求体只承载业务明文 JSON，或者只承载 CryptoEnvelope.data 密文载荷。</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
@Component
public class CryptoFilter extends OncePerRequestFilter {

    public static final String APP_ID_HEADER = "X-App-Id";
    public static final String CRYPTO_ALGORITHM_HEADER = "X-Crypto-Algorithm";
    public static final String CRYPTO_ENCRYPTED_KEY_HEADER = "X-Crypto-Encrypted-Key";
    public static final String CRYPTO_IV_HEADER = "X-Crypto-IV";

    private static final String CRYPTO_CONTEXT_ATTRIBUTE = CryptoFilter.class.getName() + ".CONTEXT";
    private static final String CRYPTO_KEY_ATTRIBUTE = CryptoFilter.class.getName() + ".KEY";
    private static final String CRYPTO_METADATA_ATTRIBUTE = CryptoFilter.class.getName() + ".METADATA";

    private final CryptoProperties properties;
    private final ObjectMapper objectMapper;
    private final CryptoKeyResolver cryptoKeyResolver;
    private final PayloadCryptoProviderRegistry providerRegistry;

    public CryptoFilter(CryptoProperties properties, ObjectMapper objectMapper,
            CryptoKeyResolver cryptoKeyResolver, PayloadCryptoProviderRegistry providerRegistry) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.cryptoKeyResolver = cryptoKeyResolver;
        this.providerRegistry = providerRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean decryptRequest = requires(request, properties.getRequestDecrypt());
        boolean encryptResponse = requires(request, properties.getResponseEncrypt());

        if (!decryptRequest && !encryptResponse) {
            filterChain.doFilter(request, response);
            return;
        }

        CryptoMetadata metadata;
        CryptoContext context;
        CryptoKey key;
        PayloadCryptoProvider provider;
        try {
            // 只要当前路径启用了请求解密或响应加密，就必须先解析协议元数据。
            metadata = resolveMetadata(request);
            context = buildContext(request);
            key = cryptoKeyResolver.resolve(context, metadata);
            provider = providerRegistry.getProvider(metadata.algorithm());

            // 算法实现可在这里准备请求级材料，例如解密后的 AES 临时密钥。
            provider.prepare(metadata, key, context);

            request.setAttribute(CRYPTO_METADATA_ATTRIBUTE, metadata);
            request.setAttribute(CRYPTO_CONTEXT_ATTRIBUTE, context);
            request.setAttribute(CRYPTO_KEY_ATTRIBUTE, key);
        } catch (BizException exception) {
            JsonResponseWriter.write(response, objectMapper, exception.getErrorCode(), exception.getMessage());
            return;
        } catch (Exception exception) {
            JsonResponseWriter.write(response, objectMapper, ErrorCode.DECRYPT_FAILED);
            return;
        }

        HttpServletRequest requestToUse = request;
        if (decryptRequest) {
            try {
                // 在 Controller 读取前，把密文请求包装成明文 JSON 请求。
                requestToUse = decryptRequest(request, metadata, key, context, provider);
            } catch (BizException exception) {
                // 解密失败发生在 Controller 前，直接返回普通错误响应，不再尝试加密错误体。
                JsonResponseWriter.write(response, objectMapper, exception.getErrorCode(), exception.getMessage());
                return;
            } catch (Exception exception) {
                JsonResponseWriter.write(response, objectMapper, ErrorCode.DECRYPT_FAILED);
                return;
            }
        }

        if (!encryptResponse) {
            filterChain.doFilter(requestToUse, response);
            return;
        }

        // 缓存下游响应，便于把 ApiResponse.data 替换为加密信封。
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(requestToUse, responseWrapper);
        } finally {
            if (!responseWrapper.isCommitted()) {
                // code/message/traceId 保持明文，只有 data 对调用方加密。
                encryptResponseSafely(requestToUse, responseWrapper);
            }
            // ContentCachingResponseWrapper 必须回写，否则缓存中的响应体不会发送给客户端。
            responseWrapper.copyBodyToResponse();
        }
    }

    private HttpServletRequest decryptRequest(HttpServletRequest request, CryptoMetadata metadata, CryptoKey key,
            CryptoContext context, PayloadCryptoProvider provider) throws IOException {
        String body = getRequestBody(request);
        if (body == null || body.isBlank()) {
            throw new BizException(ErrorCode.ENCRYPTED_BODY_INVALID);
        }

        CryptoEnvelope envelope;
        try {
            envelope = objectMapper.readValue(body, CryptoEnvelope.class);
        } catch (Exception exception) {
            throw new BizException(ErrorCode.ENCRYPTED_BODY_INVALID);
        }
        if (envelope.data() == null || envelope.data().isBlank()) {
            throw new BizException(ErrorCode.ENCRYPTED_BODY_INVALID);
        }

        String plainText = provider.decrypt(envelope, metadata, key, context);
        if (plainText == null || plainText.isBlank()) {
            throw new BizException(ErrorCode.ENCRYPTED_BODY_INVALID);
        }

        request.setAttribute(RequestLogAttributes.PLAIN_REQUEST_BODY, plainText);

        // Controller 和参数校验层看到的是和未启用传输加密时一致的 JSON 结构。
        return new DecryptedBodyHttpServletRequest(request, plainText.getBytes(StandardCharsets.UTF_8));
    }

    private void encryptResponseSafely(HttpServletRequest request, ContentCachingResponseWrapper responseWrapper)
            throws IOException {
        try {
            encryptResponseIfNecessary(request, responseWrapper);
        } catch (BizException exception) {
            responseWrapper.resetBuffer();
            JsonResponseWriter.write(responseWrapper, objectMapper, exception.getErrorCode(), exception.getMessage());
        } catch (Exception exception) {
            responseWrapper.resetBuffer();
            JsonResponseWriter.write(responseWrapper, objectMapper, ErrorCode.ENCRYPT_FAILED);
        }
    }

    private void encryptResponseIfNecessary(HttpServletRequest request, ContentCachingResponseWrapper responseWrapper)
            throws IOException {
        byte[] responseBody = responseWrapper.getContentAsByteArray();
        if (responseBody.length == 0) {
            return;
        }

        JsonNode root;
        try {
            root = objectMapper.readTree(responseBody);
        } catch (Exception exception) {
            return;
        }
        if (!(root instanceof ObjectNode objectNode)) {
            return;
        }

        JsonNode dataNode = objectNode.get("data");
        if (dataNode == null || dataNode.isNull()) {
            return;
        }

        CryptoMetadata metadata = getRequiredMetadata(request);
        CryptoContext context = getRequiredContext(request);
        CryptoKey key = getRequiredKey(request);
        PayloadCryptoProvider provider = providerRegistry.getProvider(metadata.algorithm());

        String plainData = objectMapper.writeValueAsString(dataNode);
        CryptoEnvelope encryptedEnvelope = provider.encrypt(plainData, metadata, key, context);

        request.setAttribute(RequestLogAttributes.PLAIN_RESPONSE_BODY, formatPlainResponseForLog(objectNode, dataNode));

        objectNode.set("data", objectMapper.valueToTree(encryptedEnvelope));
        byte[] encryptedBody = objectMapper.writeValueAsBytes(objectNode);
        responseWrapper.resetBuffer();
        responseWrapper.setCharacterEncoding(StandardCharsets.UTF_8.name());
        responseWrapper.setContentType(MediaType.APPLICATION_JSON_VALUE);
        responseWrapper.getOutputStream().write(encryptedBody);
    }

    private String formatPlainResponseForLog(ObjectNode responseNode, JsonNode plainDataNode) throws IOException {
        ObjectNode plainResponseNode = responseNode.deepCopy();
        plainResponseNode.set("data", plainDataNode);
        return objectMapper.writeValueAsString(plainResponseNode);
    }

    private CryptoMetadata resolveMetadata(HttpServletRequest request) {
        String algorithm = request.getHeader(CRYPTO_ALGORITHM_HEADER);
        if (algorithm == null || algorithm.isBlank()) {
            throw new BizException(ErrorCode.ENCRYPTED_BODY_INVALID, CRYPTO_ALGORITHM_HEADER + " is required");
        }
        return new CryptoMetadata(
                algorithm,
                request.getHeader(CRYPTO_ENCRYPTED_KEY_HEADER),
                request.getHeader(CRYPTO_IV_HEADER));
    }

    private CryptoMetadata getRequiredMetadata(HttpServletRequest request) {
        Object attribute = request.getAttribute(CRYPTO_METADATA_ATTRIBUTE);
        if (attribute instanceof CryptoMetadata metadata) {
            return metadata;
        }
        throw new BizException(ErrorCode.ENCRYPT_FAILED, "Crypto metadata is missing");
    }

    private CryptoContext getRequiredContext(HttpServletRequest request) {
        Object attribute = request.getAttribute(CRYPTO_CONTEXT_ATTRIBUTE);
        if (attribute instanceof CryptoContext context) {
            return context;
        }
        throw new BizException(ErrorCode.ENCRYPT_FAILED, "Crypto context is missing");
    }

    private CryptoKey getRequiredKey(HttpServletRequest request) {
        Object attribute = request.getAttribute(CRYPTO_KEY_ATTRIBUTE);
        if (attribute instanceof CryptoKey key) {
            return key;
        }
        throw new BizException(ErrorCode.ENCRYPT_FAILED, "Crypto key is missing");
    }

    private CryptoContext buildContext(HttpServletRequest request) {
        CryptoContext context = new CryptoContext();
        context.setAppId(request.getHeader(APP_ID_HEADER));
        context.setMethod(request.getMethod());
        context.setPath(request.getRequestURI());
        context.setTraceId(TraceIdUtils.getTraceId());
        return context;
    }

    private String getRequestBody(HttpServletRequest request) throws IOException {
        if (request instanceof CachedBodyHttpServletRequest cachedRequest) {
            return cachedRequest.getCachedBodyAsString();
        }
        return new String(request.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    private boolean requires(HttpServletRequest request, CryptoProperties.PathRule rule) {
        if (!properties.isEnabled() || rule == null || !rule.isEnabled() || PathMatchUtils.isOptions(request)) {
            return false;
        }
        String path = request.getRequestURI();
        if (PathMatchUtils.matchesAny(rule.getExcludePaths(), path)) {
            return false;
        }
        return PathMatchUtils.matchesAny(rule.getIncludePaths(), path);
    }
}
