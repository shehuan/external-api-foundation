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
 * <p>安全协议元数据统一放在 Header 中。</p>
 * <p>X-App-Id / X-Crypto-Algorithm / X-Crypto-Encrypted-Key / X-Crypto-IV。</p>
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
        // 第一步：请求解密、响应加密分别按路径规则判断。
        boolean decryptRequest = requires(request, properties.getRequestDecrypt());
        boolean encryptResponse = requires(request, properties.getResponseEncrypt());

        // 第二步：当前接口不涉及加解密时直接放行。
        if (!decryptRequest && !encryptResponse) {
            filterChain.doFilter(request, response);
            return;
        }

        CryptoMetadata metadata;
        CryptoContext context;
        CryptoKey key;
        PayloadCryptoProvider provider;
        try {
            // 第三步：只要请求解密或响应加密任一生效，就必须从 Header 解析本次加密元数据。
            metadata = resolveMetadata(request);
            context = buildContext(request);
            key = cryptoKeyResolver.resolve(context, metadata);
            provider = providerRegistry.getProvider(metadata.algorithm());

            // 第四步：给具体算法一次准备会话的机会。
            // 混合加密可在这里解开 encryptedKey，并把临时对称密钥放入 context.attributes。
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
                // 第五步：如果当前路径要求请求解密，则把请求体 CryptoEnvelope.data 解密成明文 JSON。
                // 解密后返回新的 HttpServletRequestWrapper，后续 Controller 读取到的是明文 JSON。
                requestToUse = decryptRequest(request, metadata, key, context, provider);
            } catch (BizException exception) {
                // 第六步：请求解密阶段发生可识别业务异常时，直接返回统一错误响应。
                // 这类错误通常发生在进入 Controller 之前，不再尝试加密错误响应。
                JsonResponseWriter.write(response, objectMapper, exception.getErrorCode(), exception.getMessage());
                return;
            } catch (Exception exception) {
                // 第七步：未知解密异常统一收敛为 DECRYPT_FAILED，避免向外暴露算法内部细节。
                JsonResponseWriter.write(response, objectMapper, ErrorCode.DECRYPT_FAILED);
                return;
            }
        }

        if (!encryptResponse) {
            // 第八步：如果当前路径不要求响应加密，则直接使用原 request 或解密后的 request 继续执行。
            // 这对应“请求明文 + 响应明文”或“请求密文 + 响应明文”两类场景。
            filterChain.doFilter(requestToUse, response);
            return;
        }

        // 第九步：当前路径要求响应加密，需要先缓存后续链路写出的响应体。
        // 如果不使用 ContentCachingResponseWrapper，Controller 写出的 body 会直接发送给客户端，无法再改写 data。
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        try {
            // 第十步：继续执行后续 Filter、Controller、全局异常处理器。
            // 如果前面执行了解密，这里传入的是明文请求体包装器。
            filterChain.doFilter(requestToUse, responseWrapper);
        } finally {
            if (!responseWrapper.isCommitted()) {
                // 第十一步：下游处理完成后，再读取缓存的 ApiResponse，并只加密其中的 data 字段。
                // code/message/traceId 保持明文，便于调用方、网关和日志快速判断请求结果。
                encryptResponseSafely(requestToUse, responseWrapper);
            }
            // 第十二步：必须调用 copyBodyToResponse，否则缓存中的响应体不会真正写回客户端。
            responseWrapper.copyBodyToResponse();
        }
    }

    private HttpServletRequest decryptRequest(HttpServletRequest request, CryptoMetadata metadata, CryptoKey key,
            CryptoContext context, PayloadCryptoProvider provider) throws IOException {
        // 第一步：请求需要解密时，请求体必须是 CryptoEnvelope。
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

        // 第二步：调用算法实现解密 data，得到明文 JSON。
        String plainText = provider.decrypt(envelope, metadata, key, context);
        if (plainText == null || plainText.isBlank()) {
            throw new BizException(ErrorCode.ENCRYPTED_BODY_INVALID);
        }

        request.setAttribute(RequestLogAttributes.PLAIN_REQUEST_BODY, plainText);

        // 第三步：把请求体替换成明文 JSON，后续 Controller 不感知加密细节。
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

        // 仍然只加密统一响应结构中的 data，code/message/traceId 保持明文。
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
