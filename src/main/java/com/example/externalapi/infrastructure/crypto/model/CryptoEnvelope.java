package com.example.externalapi.infrastructure.crypto.model;

/**
 * 加密载荷包装对象。
 *
 * <p>当前协议把算法、密钥协商、IV 等元数据统一放在 Header 中：</p>
 * <p>X-Crypto-Algorithm / X-Crypto-Encrypted-Key / X-Crypto-IV。</p>
 *
 * <p>因此 CryptoEnvelope 只负责承载密文业务数据，不再承载 algorithm、encryptedKey、iv。</p>
 *
 * @param data 加密后的业务数据；NOOP 模式下这里可以直接放明文 JSON 字符串
 */
public record CryptoEnvelope(
        String data
) {
}
