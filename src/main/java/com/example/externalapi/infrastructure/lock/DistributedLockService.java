package com.example.externalapi.infrastructure.lock;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁服务接口�? *
 * <p>用于保护�?JVM、跨实例的临界区，典型场景包括幂等记录读写、同一资源串行更新、定时任务防并发�?/p>
 * <p>注意：分布式锁只解决“同一时间只有一个执行者”的问题，不等同于幂等�?/p>
 */
public interface DistributedLockService {

    /**
     * 在锁内执行一段有返回值的业务逻辑�?     *
     * @param lockKey �?key，不同资源必须使用不�?key
     * @param waitTime 最多等待拿锁的时间
     * @param leaseTime 锁自动释放时间，防止服务宕机后死�?     * @param timeUnit 时间单位
     * @param supplier 拿到锁后要执行的业务逻辑
     * @return supplier 的返回�?     */
    <T> T execute(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Supplier<T> supplier);

    /**
     * 在锁内执行一段无返回值的业务逻辑�?     */
    void execute(String lockKey, long waitTime, long leaseTime, TimeUnit timeUnit, Runnable runnable);
}
