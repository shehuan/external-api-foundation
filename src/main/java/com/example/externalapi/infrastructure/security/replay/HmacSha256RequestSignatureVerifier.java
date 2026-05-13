package com.example.externalapi.infrastructure.security.replay;

import org.springframework.stereotype.Component;

/**
 * HMAC-SHA256 请求签名验证器。
 *
 * <p>在 HMAC 场景中，{@code signatureKey} 表示客户端和服务端共享的签名密钥。</p>
 */
@Component
public class HmacSha256RequestSignatureVerifier implements RequestSignatureVerifier {

    public static final String ALGORITHM = "HMAC_SHA256";

    @Override
    public String algorithm() {
        return ALGORITHM;
    }

    @Override
    public boolean verify(SignatureVerifyContext context) {
        String expectedSign = SignatureUtils.hmacSha256Base64(context.signingContent(), context.signatureKey());
        return SignatureUtils.constantTimeEquals(expectedSign, context.providedSign());
    }
}
