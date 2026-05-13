package com.example.externalapi.infrastructure.security.replay;

import java.time.Duration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis Nonce 存储。
 *
 * <p>Nonce 是一次性随机数。请求重放防护要求同一个 appId 下，同一个 nonce 在有效期内只能使用一次。</p>
 */
@Component
public class RedisNonceStore {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisNonceStore(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean markIfAbsent(String appId, String nonce, Duration ttl) {
        // 使用 appId 隔离不同调用方，避免不同调用方生成相同 nonce 时互相影响。
        String key = "replay:" + appId + ":" + nonce;

        // SET NX EX 只在 key 不存在时成功；false 表示 nonce 已被使用过。
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
        return Boolean.TRUE.equals(success);
    }
}
