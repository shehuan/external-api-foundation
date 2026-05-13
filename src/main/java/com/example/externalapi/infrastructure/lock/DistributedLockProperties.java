package com.example.externalapi.infrastructure.lock;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 分布式锁默认配置。
 *
 * <p>配置前缀：app.distributed-lock。</p>
 */
@ConfigurationProperties(prefix = "app.distributed-lock")
public class DistributedLockProperties {

    /**
     * 分布式锁模块开关。当前服务实现中主要预留给业务层按需判断。
     */
    private boolean enabled = true;

    /**
     * 默认最多等待拿锁时间，单位秒。
     */
    private long defaultWaitTimeSeconds = 3;

    /**
     * 默认锁租约时间，单位秒。超过该时间 Redisson 会自动释放锁。
     */
    private long defaultLeaseTimeSeconds = 10;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getDefaultWaitTimeSeconds() {
        return defaultWaitTimeSeconds;
    }

    public void setDefaultWaitTimeSeconds(long defaultWaitTimeSeconds) {
        this.defaultWaitTimeSeconds = defaultWaitTimeSeconds;
    }

    public long getDefaultLeaseTimeSeconds() {
        return defaultLeaseTimeSeconds;
    }

    public void setDefaultLeaseTimeSeconds(long defaultLeaseTimeSeconds) {
        this.defaultLeaseTimeSeconds = defaultLeaseTimeSeconds;
    }
}
