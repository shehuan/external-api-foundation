package com.example.externalapi.infrastructure.crypto.model;

/**
 * 一次请求的加密协议元数据。
 *
 * <p>该对象统一由 Header 构造，不再从请求体中解析算法和密钥协商信息。</p>
 *
 * @param algorithm 算法名称，用于选择 PayloadCryptoProvider，例如 NOOP、AES、RSA_AES
 * @param encryptedKey 可选字段，混合加密时传被服务端公钥加密后的临时对称密钥
 * @param iv 可选字段，初始化向量或 nonce；如果明文传递，必须纳入签名或算法认证范围
 */
public record CryptoMetadata(
        String algorithm,
        String encryptedKey,
        String iv
) {
}
