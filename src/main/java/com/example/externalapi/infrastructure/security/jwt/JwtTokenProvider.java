package com.example.externalapi.infrastructure.security.jwt;

import com.example.externalapi.infrastructure.security.user.LoginUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * JWT 生成与解析工具。
 *
 * <p>职责边界：</p>
 * <p>1. 负责生成 token。</p>
 * <p>2. 负责校验签名、issuer、过期时间。</p>
 * <p>3. 负责从 token claims 中还原 LoginUser。</p>
 * <p>它不负责判断某个接口是否需要鉴权，接口鉴权由 JwtAuthFilter 负责。</p>
 */
@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        // JJWT 要求 HMAC 密钥长度满足算法安全要求。配置中使用 Base64 字符串，启动时解码成 SecretKey。
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.getSecret()));
    }

    /**
     * 生成访问 token。
     *
     * <p>当前 token 中保存 userId、username、roles。敏感信息不要放入 JWT，因为 JWT 只是签名，不是加密。</p>
     */
    public String generateToken(LoginUser loginUser) {
        // 第一步：计算签发时间和过期时间。
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.getAccessTokenExpireMinutes(), ChronoUnit.MINUTES);

        // 第二步：构造 JWT claims。
        // subject 是 userId，这是业界常见约定；username/roles 作为自定义 claim。
        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(String.valueOf(loginUser.userId()))
                .claim("username", loginUser.username())
                .claim("roles", loginUser.roles())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                // jti 用于标识一个 token，后续如果做 token 黑名单或审计可以使用。
                .id(java.util.UUID.randomUUID().toString())
                // 第三步：使用服务端密钥签名，防止 token 被篡改。
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析 token 并还原当前登录用户。
     */
    public LoginUser parseLoginUser(String token) {
        // 第一步：解析并校验 claims。签名错误、issuer 错误、过期都会在这里抛异常。
        Claims claims = parseClaims(token);

        // 第二步：从标准 subject 中读取 userId。
        Long userId = Long.valueOf(claims.getSubject());

        // 第三步：从自定义 claims 中读取 username 和 roles。
        String username = claims.get("username", String.class);
        List<String> roles = claims.get("roles", List.class);
        return new LoginUser(userId, username, roles == null ? List.of() : roles);
    }

    /**
     * 解析 JWT claims。
     *
     * <p>该方法会完成签名校验、issuer 校验、过期时间校验。</p>
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                // 使用同一把密钥校验签名。
                .verifyWith(secretKey)
                // 要求 issuer 与配置一致，避免其他系统签发的 token 被误用。
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 判断 token 是否有效。
     *
     * <p>适合在非 Filter 场景中做轻量校验。接口鉴权主流程仍使用 JwtAuthFilter。</p>
     */
    public boolean isValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }
}
