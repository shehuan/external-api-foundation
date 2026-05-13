package com.example.externalapi.infrastructure.crypto.provider;

import com.example.externalapi.infrastructure.crypto.context.CryptoContext;
import com.example.externalapi.infrastructure.crypto.model.CryptoEnvelope;
import com.example.externalapi.infrastructure.crypto.model.CryptoKey;
import com.example.externalapi.infrastructure.crypto.model.CryptoMetadata;

/**
 * 请求解密和响应加密算法扩展点。
 *
 * <p>框架负责路径匹配、Header 元数据解析、密钥解析、请求体替换和响应体改写；
 * 真实算法只需要实现本接口，例如 AES、RSA_AES、SM4、SM2_SM4。</p>
 */
public interface PayloadCryptoProvider {

    /**
     * 判断当前 Provider 是否支持 Header 中声明的算法。
     */
    boolean supports(String algorithm);

    /**
     * 准备本次加解密会话。
     *
     * <p>该方法在请求解密或响应加密之前调用。混合加密场景可在这里解开临时对称密钥，
     * 并放入 {@link CryptoContext#getAttributes()}，供 {@link #decrypt} 和 {@link #encrypt} 复用。</p>
     */
    default void prepare(CryptoMetadata metadata, CryptoKey key, CryptoContext context) {
    }

    /**
     * 解密请求数据，返回 Controller 能识别的明文 JSON 字符串。
     */
    String decrypt(CryptoEnvelope envelope, CryptoMetadata metadata, CryptoKey key, CryptoContext context);

    /**
     * 加密响应 data JSON 字符串，返回响应密文载荷。
     */
    CryptoEnvelope encrypt(String plainText, CryptoMetadata metadata, CryptoKey key, CryptoContext context);
}
