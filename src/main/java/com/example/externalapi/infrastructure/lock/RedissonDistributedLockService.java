package com.example.externalapi.infrastructure.lock;

import com.example.externalapi.common.error.ErrorCode;
import com.example.externalapi.common.exception.BizException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * Redisson 分布式锁实现。
 *
 * <p>使用 Redisson 的 {@code RLock.tryLock(waitTime, leaseTime, unit)}。</p>
 * <p>waitTime 控制最多等待多久拿锁；超过时间仍未拿到锁，会抛出 TOO_MANY_REQUESTS。</p>
 * <p>leaseTime 控制锁最多持有多久自动释放，避免持锁服务异常退出导致死锁。</p>
 */
@Service
public class RedissonDistributedLockService implements DistributedLockService {

    private final RedissonClient redissonClient;

    public RedissonDistributedLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> T execute(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> supplier) {
        // 相同 lockKey 在多个服务实例之间竞争同一把分布式锁。
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked;
        try {
            locked = lock.tryLock(waitTime, leaseTime, timeUnit);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS, "Failed to acquire distributed lock");
        }
        if (!locked) {
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS, "Failed to acquire distributed lock");
        }
        try {
            return supplier.get();
        } finally {
            // 只允许持锁线程释放锁，避免误释放其他线程或其他实例的锁。
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public void execute(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Runnable runnable) {
        execute(lockKey, waitTime, leaseTime, timeUnit, () -> {
            runnable.run();
            return null;
        });
    }
}
