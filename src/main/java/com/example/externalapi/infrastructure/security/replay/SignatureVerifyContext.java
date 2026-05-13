package com.example.externalapi.infrastructure.security.replay;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 请求签名验证上下文。
 *
 * <p>{@code ReplayProtectionFilter} 负责收集这些数据，具体算法实现只负责根据上下文判断签名是否正确。</p>
 *
 * @param appId 调用方应用标识
 * @param signatureKey 调用方签名密钥、公钥或密钥引用，具体含义由验证算法决定
 * @param providedSign 调用方传入的签名
 * @param signingContent 服务端构造的签名原文
 * @param request 原始 HTTP 请求，预留给复杂算法读取更多上下文
 */
public record SignatureVerifyContext(
        String appId,
        String signatureKey,
        String providedSign,
        String signingContent,
        HttpServletRequest request
) {
}
