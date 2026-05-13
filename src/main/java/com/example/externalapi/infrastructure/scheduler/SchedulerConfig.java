package com.example.externalapi.infrastructure.scheduler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;

/**
 * 定时任务基础配置。
 *
 * <p>开启 Spring @Scheduled，并指定统一线程池，避免所有定时任务共用默认单线程。</p>
 * <p>通过 app.scheduler.enabled 可以整体关闭定时任务能力。</p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(prefix = "app.scheduler", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerConfig implements SchedulingConfigurer {

    private final TaskScheduler taskScheduler;

    public SchedulerConfig(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    @Bean(destroyMethod = "shutdown")
    public static ThreadPoolTaskScheduler taskScheduler(SchedulerProperties properties) {
        // ThreadPoolTaskScheduler 是 @Scheduled 的底层执行器。
        // 这里显式配置线程池大小、线程名前缀和优雅关闭行为，方便生产排查和停机。
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(properties.getPoolSize());
        scheduler.setThreadNamePrefix(properties.getThreadNamePrefix());
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(properties.getAwaitTerminationSeconds());
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 把自定义线程池注册给 Spring 定时任务框架。
        taskRegistrar.setTaskScheduler(taskScheduler);
    }
}
