package com.example.externalapi.task;

import com.example.externalapi.common.error.ErrorCode;
import com.example.externalapi.common.exception.BizException;
import com.example.externalapi.infrastructure.lock.DistributedLockService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 多实例互斥定时任务示例。
 *
 * <p>每个实例都会按 fixedDelay 触发任务，但只有拿到 Redisson 分布式锁的实例会真正执行。</p>
 * <p>waitTime = 0 表示拿不到锁就跳过本次调度，不排队等待。</p>
 */
@Component
@ConditionalOnProperty(prefix = "app.task.distributed-serial-demo", name = "enabled", havingValue = "true")
public class DistributedSerialDemoTask {

    private static final Logger log = LoggerFactory.getLogger(DistributedSerialDemoTask.class);
    private static final String LOCK_KEY = "scheduler:distributed-serial-demo";

    private final DistributedLockService distributedLockService;

    public DistributedSerialDemoTask(DistributedLockService distributedLockService) {
        this.distributedLockService = distributedLockService;
    }

    @Scheduled(fixedDelayString = "${app.task.distributed-serial-demo.fixed-delay:5000}")
    public void execute() {
        try {
            distributedLockService.execute(LOCK_KEY, 0, 300, TimeUnit.SECONDS, this::doExecute);
        } catch (BizException exception) {
            if (exception.getErrorCode() == ErrorCode.TOO_MANY_REQUESTS) {
                log.info("Distributed serial demo task skipped because another instance is running");
                return;
            }
            throw exception;
        }
    }

    private void doExecute() {
        log.info("Distributed serial demo task started");
        try {
            // 示例任务逻辑：真实业务中替换为集群内只能单实例执行的同步、补偿、结算等操作。
            Thread.sleep(1000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Distributed serial demo task interrupted");
            return;
        }
        log.info("Distributed serial demo task finished");
    }
}
