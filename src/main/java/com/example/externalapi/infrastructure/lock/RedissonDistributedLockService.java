package com.example.externalapi.infrastructure.lock;

import com.example.externalapi.common.error.ErrorCode;
import com.example.externalapi.common.exception.BizException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

/**
 * Redisson 分布式锁实现�? *
 * <p>使用 Redisson �?RLock.tryLock(waitTime, leaseTime, unit)�?/p>
 * <p>waitTime 控制最多等待多久拿锁；超过时间仍未拿到锁，会抛�?TOO_MANY_REQUESTS�?/p>
 * <p>leaseTime 控制锁最多持有多久自动释放，避免持锁服务异常退出导致死锁�?/p>
 */
@Service
public class RedissonDistributedLockService implements DistributedLockService {

    private final RedissonClient redissonClient;

    public RedissonDistributedLockService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public <T> T execute(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> supplier) {
        // 第一步：根据业务 key 获取一把分布式锁。同一�?lockKey 会竞争同一把锁�?
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked;
        try {
            // 第二步：尝试拿锁。最多等�?waitTime，拿到后 leaseTime 到期自动释放�?
            locked = lock.tryLock(waitTime, leaseTime, timeUnit);
        } catch (InterruptedException exception) {
            // 第三步：线程被中断时恢复中断标记，并转换成统一业务异常�?
            Thread.currentThread().interrupt();
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS, "Failed to acquire distributed lock");
        }
        if (!locked) {
            // 第四步：超过 waitTime 仍未拿到锁，说明当前资源竞争较强，直接失败�?
            throw new BizException(ErrorCode.TOO_MANY_REQUESTS, "Failed to acquire distributed lock");
        }
        try {
            // 第五步：只有拿到锁的线程才能执行真正的业务逻辑�?
            return supplier.get();
        } finally {
            // 第六步：只释放当前线程持有的锁，避免误释放其他线程或其他实例的锁�?
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
