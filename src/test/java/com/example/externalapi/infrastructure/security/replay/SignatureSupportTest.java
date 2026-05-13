package com.example.externalapi.infrastructure.security.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.externalapi.common.error.ErrorCode;
import com.example.externalapi.common.exception.BizException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 验证签名工具、HMAC 验签器和验签器注册表的核心行为。
 */
class SignatureSupportTest {

    @Test
    void sha256HexShouldUseEmptyBytesWhenInputIsNull() {
        // 空请求体也必须有稳定摘要，签名双方才能对空 body 使用同一规则。
        assertEquals(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                SignatureUtils.sha256Hex(null));
    }

    @Test
    void sha256HexShouldCalculateKnownDigest() {
        assertEquals(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                SignatureUtils.sha256Hex("abc".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void constantTimeEqualsShouldRejectNullAndDifferentValues() {
        assertTrue(SignatureUtils.constantTimeEquals("same", "same"));
        assertFalse(SignatureUtils.constantTimeEquals("same", "other"));
        assertFalse(SignatureUtils.constantTimeEquals(null, "same"));
        assertFalse(SignatureUtils.constantTimeEquals("same", null));
    }

    @Test
    void hmacVerifierShouldVerifyExpectedSignature() {
        HmacSha256RequestSignatureVerifier verifier = new HmacSha256RequestSignatureVerifier();
        String signingContent = "POST\n/api/orders\nbodyHash";
        String sign = SignatureUtils.hmacSha256Base64(signingContent, "secret");

        assertTrue(verifier.verify(new SignatureVerifyContext(
                "partner-a", "secret", sign, signingContent, null)));
        assertFalse(verifier.verify(new SignatureVerifyContext(
                "partner-a", "secret", "bad-sign", signingContent, null)));
    }

    @Test
    void registryShouldFindVerifierIgnoringCaseAndSpaces() {
        // 配置文件中常见大小写或首尾空格差异，注册表应统一归一化。
        RequestSignatureVerifier verifier = new HmacSha256RequestSignatureVerifier();
        RequestSignatureVerifierRegistry registry = new RequestSignatureVerifierRegistry(List.of(verifier));

        assertSame(verifier, registry.getVerifier(" hmac_sha256 "));
    }

    @Test
    void registryShouldThrowWhenAlgorithmUnsupported() {
        RequestSignatureVerifierRegistry registry = new RequestSignatureVerifierRegistry(List.of());

        BizException exception = assertThrows(BizException.class, () -> registry.getVerifier("UNKNOWN"));

        assertEquals(ErrorCode.SIGN_INVALID, exception.getErrorCode());
        assertEquals("Unsupported signature algorithm", exception.getMessage());
    }
}
