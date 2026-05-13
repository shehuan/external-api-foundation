package com.example.externalapi.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 单实例串行定时任务示例。
 *
 * <p>fixedDelay 的语义是“上一次执行完成后，再等待指定时间执行下一次”。</p>
 * <p>因此在单 JVM 内，同一个任务不会重叠执行。</p>
 */
@Component
@ConditionalOnProperty(prefix = "app.task.local-serial-demo", name = "enabled", havingValue = "true")
public class LocalSerialDemoTask {

    private static final Logger log = LoggerFactory.getLogger(LocalSerialDemoTask.class);

    @Scheduled(fixedDelayString = "${app.task.local-serial-demo.fixed-delay:5000}")
    public void execute() {
        log.info("Local serial demo task started");
        try {
            // 示例任务逻辑：真实业务中替换为清理、同步、补偿等操作。
            Thread.sleep(1000);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Local serial demo task interrupted");
            return;
        }
        log.info("Local serial demo task finished");
    }
}
