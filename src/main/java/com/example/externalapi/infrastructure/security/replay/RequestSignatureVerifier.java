package com.example.externalapi.infrastructure.security.replay;

/**
 * 请求签名验证算法扩展点。
 *
 * <p>调用方不通过请求参数选择算法，服务端根据配置选择算法。新增 RSA、SM2 等算法时，
 * 实现该接口并注册为 Spring Bean 即可。</p>
 */
public interface RequestSignatureVerifier {

    /**
     * 服务端配置中使用的算法名称。
     */
    String algorithm();

    /**
     * 验证调用方签名是否正确。
     */
    boolean verify(SignatureVerifyContext context);
}
