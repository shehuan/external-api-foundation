package com.example.externalapi.infrastructure.crypto.model;

import java.util.Map;

/**
 * 密钥解析结果�? *
 * <p>CryptoKey 只表示密钥材料或密钥引用，不再承担算法选择职责�?/p>
 * <p>算法来自 Header 中的 X-Crypto-Algorithm，并封装�?CryptoMetadata 中�?/p>
 *
 * @param appId 调用方应用标�? * @param secret 配置中的共享密钥、私钥、KMS keyId 或其他密钥引�? * @param metadata 预留扩展字段
 */
public record CryptoKey(
        String appId,
        String secret,
        Map<String, String> metadata
) {
}
