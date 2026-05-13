package com.example.externalapi.infrastructure.scheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 定时任务配置项。
 *
 * <p>配置前缀：app.scheduler。</p>
 */
@ConfigurationProperties(prefix = "app.scheduler")
public class SchedulerProperties {

    /**
     * 定时任务总开关。关闭后 @Scheduled 不会被启用。
     */
    private boolean enabled = true;

    /**
     * 定时任务线程池大小。避免多个定时任务共用 Spring 默认单线程。
     */
    private int poolSize = 4;

    /**
     * 定时任务线程名前缀，方便通过日志和线程 dump 定位任务线程。
     */
    private String threadNamePrefix = "demo-scheduler-";

    /**
     * 应用关闭时，最多等待正在执行的任务多少秒。
     */
    private int awaitTerminationSeconds = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public void setPoolSize(int poolSize) {
        this.poolSize = poolSize;
    }

    public String getThreadNamePrefix() {
        return threadNamePrefix;
    }

    public void setThreadNamePrefix(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    public int getAwaitTerminationSeconds() {
        return awaitTerminationSeconds;
    }

    public void setAwaitTerminationSeconds(int awaitTerminationSeconds) {
        this.awaitTerminationSeconds = awaitTerminationSeconds;
    }
}
