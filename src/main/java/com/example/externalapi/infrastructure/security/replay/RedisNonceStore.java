package com.example.externalapi.infrastructure.security.replay;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Nonce 存储�? *
 * <p>Nonce 是一次性随机数。请求重放防护要求同一�?appId 下，同一�?nonce 在有效期内只能使用一次�?/p>
 */
@Component
public class RedisNonceStore {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisNonceStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean markIfAbsent(String appId, String nonce, Duration ttl) {
        // 使用 appId 隔离不同调用方，避免不同调用方生成相�?nonce 时互相影响�?
        String key = "replay:" + appId + ":" + nonce;

        // SET NX EX：只�?key 不存在时写入成功，并设置过期时间�?        // 返回 true 表示 nonce 第一次出现；返回 false 表示疑似重放请求�?
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(success);
    }
}
