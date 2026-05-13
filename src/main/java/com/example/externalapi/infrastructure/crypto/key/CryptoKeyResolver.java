package com.example.externalapi.infrastructure.crypto.key;

import com.example.externalapi.infrastructure.crypto.context.CryptoContext;
import com.example.externalapi.infrastructure.crypto.model.CryptoKey;
import com.example.externalapi.infrastructure.crypto.model.CryptoMetadata;

/**
 * 密钥解析器。
 *
 * <p>职责：根据 appId 和本次加密元数据解析密钥材料。</p>
 * <p>默认实现从 application.yml 的 app.crypto.clients 查找，后续可替换为数据库、KMS、租户中心。</p>
 */
public interface CryptoKeyResolver {

    /**
     * 解析本次请求需要的密钥材料。
     */
    CryptoKey resolve(CryptoContext context, CryptoMetadata metadata);
}
