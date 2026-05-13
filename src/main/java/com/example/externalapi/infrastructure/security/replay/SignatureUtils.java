package com.example.externalapi.infrastructure.security.replay;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 请求签名和摘要工具。
 *
 * <p>当前用于请求防重放：请求体先计算 SHA-256 摘要，再把摘要和请求元数据组成签名原文，
 * 最后使用 HMAC-SHA256 生成签名。签名比较使用常量时间比较，降低时序攻击风险。</p>
 */
public final class SignatureUtils {

    private SignatureUtils() {
    }

    public static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(bytes == null ? new byte[0] : bytes);
            return toHex(hashed);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to calculate SHA-256", exception);
        }
    }

    public static String hmacSha256Base64(String content, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to calculate HMAC-SHA256", exception);
        }
    }

    public static boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
