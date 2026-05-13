package com.example.externalapi.infrastructure.crypto.provider;

import com.example.externalapi.infrastructure.crypto.context.CryptoContext;
import com.example.externalapi.infrastructure.crypto.model.CryptoEnvelope;
import com.example.externalapi.infrastructure.crypto.model.CryptoKey;
import com.example.externalapi.infrastructure.crypto.model.CryptoMetadata;
import org.springframework.stereotype.Component;

/**
 * NOOP 加解密实现。
 *
 * <p>NOOP 表示 No Operation，即不做真实加解密，只用于验证框架流程或本地联调。生产环境不应把
 * NOOP 当作安全加密算法使用。</p>
 */
@Component
public class NoopPayloadCryptoProvider implements PayloadCryptoProvider {

    public static final String ALGORITHM = "NOOP";

    @Override
    public boolean supports(String algorithm) {
        return algorithm == null || algorithm.isBlank() || ALGORITHM.equalsIgnoreCase(algorithm);
    }

    @Override
    public String decrypt(CryptoEnvelope envelope, CryptoMetadata metadata, CryptoKey key, CryptoContext context) {
        return envelope.data();
    }

    @Override
    public CryptoEnvelope encrypt(String plainText, CryptoMetadata metadata, CryptoKey key, CryptoContext context) {
        return new CryptoEnvelope(plainText);
    }
}
