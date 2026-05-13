package com.example.externalapi.infrastructure.security.replay;

import com.example.externalapi.common.error.ErrorCode;
import com.example.externalapi.common.exception.BizException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 请求签名验证器注册表。
 *
 * <p>Spring 会自动收集所有 {@link RequestSignatureVerifier} 实现。运行时根据服务端配置的
 * 算法名称选择对应验证器，而不是信任调用方传入的算法。</p>
 */
@Component
public class RequestSignatureVerifierRegistry {

    private final Map<String, RequestSignatureVerifier> verifiers;

    public RequestSignatureVerifierRegistry(List<RequestSignatureVerifier> verifiers) {
        this.verifiers = verifiers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        verifier -> normalize(verifier.algorithm()),
                        Function.identity()));
    }

    public RequestSignatureVerifier getVerifier(String algorithm) {
        RequestSignatureVerifier verifier = verifiers.get(normalize(algorithm));
        if (verifier == null) {
            throw new BizException(ErrorCode.SIGN_INVALID, "Unsupported signature algorithm");
        }
        return verifier;
    }

    private String normalize(String algorithm) {
        return algorithm == null ? "" : algorithm.trim().toUpperCase(Locale.ROOT);
    }
}
