package com.example.externalapi.infrastructure.crypto.key;

import com.example.externalapi.common.error.ErrorCode;
import com.example.externalapi.common.exception.BizException;
import com.example.externalapi.infrastructure.crypto.config.CryptoProperties;
import com.example.externalapi.infrastructure.crypto.context.CryptoContext;
import com.example.externalapi.infrastructure.crypto.model.CryptoKey;
import com.example.externalapi.infrastructure.crypto.model.CryptoMetadata;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 默认密钥解析器。
 *
 * <p>当前版本按 {@code X-App-Id} 在 {@code app.crypto.clients} 中查找密钥。
 * 生产环境如果需要密钥轮换、KMS 托管或多租户隔离，可以替换该实现。</p>
 */
@Component
public class DefaultCryptoKeyResolver implements CryptoKeyResolver {

    private final CryptoProperties properties;

    public DefaultCryptoKeyResolver(CryptoProperties properties) {
        this.properties = properties;
    }

    @Override
    public CryptoKey resolve(CryptoContext context, CryptoMetadata metadata) {
        String appId = context.getAppId();
        if (appId == null || appId.isBlank()) {
            throw new BizException(ErrorCode.APP_ID_INVALID);
        }

        // 未配置 clients 时允许框架先跑通，适合 NOOP 模式或本地联调。
        if (properties.getClients() == null || properties.getClients().isEmpty()) {
            return new CryptoKey(appId, null, Map.of());
        }

        return properties.getClients().stream()
                .filter(CryptoProperties.Client::isEnabled)
                .filter(client -> appId.equals(client.getAppId()))
                .findFirst()
                .map(client -> new CryptoKey(appId, client.getSecret(), Map.of()))
                .orElseThrow(() -> new BizException(ErrorCode.CRYPTO_KEY_NOT_FOUND));
    }
}
