package com.example.externalapi.infrastructure.crypto.provider;

import com.example.externalapi.common.error.ErrorCode;
import com.example.externalapi.common.exception.BizException;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 加解密 Provider 注册表。
 *
 * <p>Spring 会自动注入所有 {@link PayloadCryptoProvider} 实现。运行时根据
 * {@code X-Crypto-Algorithm} 选择第一个支持当前算法的 Provider。</p>
 */
@Component
public class PayloadCryptoProviderRegistry {

    private final List<PayloadCryptoProvider> providers;

    public PayloadCryptoProviderRegistry(List<PayloadCryptoProvider> providers) {
        this.providers = providers;
    }

    public PayloadCryptoProvider getProvider(String algorithm) {
        return providers.stream()
                .filter(provider -> provider.supports(algorithm))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.CRYPTO_PROVIDER_NOT_FOUND));
    }
}
