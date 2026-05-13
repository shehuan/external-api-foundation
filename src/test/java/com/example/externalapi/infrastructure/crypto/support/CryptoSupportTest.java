package com.example.externalapi.infrastructure.crypto.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.externalapi.common.error.ErrorCode;
import com.example.externalapi.common.exception.BizException;
import com.example.externalapi.infrastructure.crypto.config.CryptoProperties;
import com.example.externalapi.infrastructure.crypto.context.CryptoContext;
import com.example.externalapi.infrastructure.crypto.key.DefaultCryptoKeyResolver;
import com.example.externalapi.infrastructure.crypto.model.CryptoEnvelope;
import com.example.externalapi.infrastructure.crypto.model.CryptoKey;
import com.example.externalapi.infrastructure.crypto.model.CryptoMetadata;
import com.example.externalapi.infrastructure.crypto.provider.NoopPayloadCryptoProvider;
import com.example.externalapi.infrastructure.crypto.provider.PayloadCryptoProvider;
import com.example.externalapi.infrastructure.crypto.provider.PayloadCryptoProviderRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 验证加解密扩展点的 Provider 选择、NOOP 行为和默认密钥解析逻辑。
 */
class CryptoSupportTest {

    @Test
    void noopProviderShouldSupportBlankAndNoopAlgorithms() {
        NoopPayloadCryptoProvider provider = new NoopPayloadCryptoProvider();

        assertTrue(provider.supports(null));
        assertTrue(provider.supports(" "));
        assertTrue(provider.supports("noop"));
        assertFalse(provider.supports("AES"));
    }

    @Test
    void noopProviderShouldPassThroughPlainText() {
        NoopPayloadCryptoProvider provider = new NoopPayloadCryptoProvider();
        CryptoMetadata metadata = new CryptoMetadata("NOOP", null, null);
        CryptoKey key = new CryptoKey("partner-a", "secret", Map.of());
        CryptoContext context = new CryptoContext();

        assertEquals("{\"id\":1}", provider.decrypt(new CryptoEnvelope("{\"id\":1}"), metadata, key, context));
        assertEquals("{\"ok\":true}", provider.encrypt("{\"ok\":true}", metadata, key, context).data());
    }

    @Test
    void providerRegistryShouldReturnFirstSupportedProvider() {
        PayloadCryptoProvider first = new NoopPayloadCryptoProvider();
        PayloadCryptoProvider second = new NoopPayloadCryptoProvider();
        PayloadCryptoProviderRegistry registry = new PayloadCryptoProviderRegistry(List.of(first, second));

        assertSame(first, registry.getProvider("NOOP"));
    }

    @Test
    void providerRegistryShouldThrowWhenProviderUnsupported() {
        PayloadCryptoProviderRegistry registry = new PayloadCryptoProviderRegistry(List.of(new NoopPayloadCryptoProvider()));

        BizException exception = assertThrows(BizException.class, () -> registry.getProvider("AES"));

        assertEquals(ErrorCode.CRYPTO_PROVIDER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void defaultKeyResolverShouldResolveEnabledClientSecret() {
        CryptoProperties properties = new CryptoProperties();
        properties.setClients(List.of(client("partner-a", "secret-a", true), client("partner-b", "secret-b", true)));
        DefaultCryptoKeyResolver resolver = new DefaultCryptoKeyResolver(properties);
        CryptoContext context = new CryptoContext();
        context.setAppId("partner-b");

        CryptoKey key = resolver.resolve(context, new CryptoMetadata("NOOP", null, null));

        assertEquals("partner-b", key.appId());
        assertEquals("secret-b", key.secret());
        assertEquals(Map.of(), key.metadata());
    }

    @Test
    void defaultKeyResolverShouldAllowNoConfiguredClients() {
        // 未配置 clients 时允许框架先跑通，便于 NOOP 模式和本地联调。
        CryptoProperties properties = new CryptoProperties();
        properties.setClients(List.of());
        DefaultCryptoKeyResolver resolver = new DefaultCryptoKeyResolver(properties);
        CryptoContext context = new CryptoContext();
        context.setAppId("partner-a");

        CryptoKey key = resolver.resolve(context, new CryptoMetadata("NOOP", null, null));

        assertEquals("partner-a", key.appId());
        assertNull(key.secret());
    }

    @Test
    void defaultKeyResolverShouldRejectMissingAppIdAndDisabledClient() {
        DefaultCryptoKeyResolver missingAppResolver = new DefaultCryptoKeyResolver(new CryptoProperties());
        CryptoContext missingAppContext = new CryptoContext();

        BizException missingApp = assertThrows(BizException.class,
                () -> missingAppResolver.resolve(missingAppContext, new CryptoMetadata("NOOP", null, null)));
        assertEquals(ErrorCode.APP_ID_INVALID, missingApp.getErrorCode());

        CryptoProperties properties = new CryptoProperties();
        properties.setClients(List.of(client("partner-a", "secret-a", false)));
        DefaultCryptoKeyResolver disabledClientResolver = new DefaultCryptoKeyResolver(properties);
        CryptoContext disabledClientContext = new CryptoContext();
        disabledClientContext.setAppId("partner-a");

        BizException disabledClient = assertThrows(BizException.class,
                () -> disabledClientResolver.resolve(disabledClientContext, new CryptoMetadata("NOOP", null, null)));
        assertEquals(ErrorCode.CRYPTO_KEY_NOT_FOUND, disabledClient.getErrorCode());
    }

    private CryptoProperties.Client client(String appId, String secret, boolean enabled) {
        CryptoProperties.Client client = new CryptoProperties.Client();
        client.setAppId(appId);
        client.setSecret(secret);
        client.setEnabled(enabled);
        return client;
    }
}
