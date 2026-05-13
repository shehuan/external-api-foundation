package com.example.externalapi.infrastructure.idempotency;

import com.example.externalapi.common.error.ErrorCode;
import com.example.externalapi.common.exception.BizException;
import com.example.externalapi.infrastructure.lock.DistributedLockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 基于 Redis 的幂等服务实现�? *
 * <p>核心思路�?/p>
 * <p>1. 使用 bizType + Idempotency-Key 组成 Redis key�?/p>
 * <p>2. 使用 requestHash 判断同一�?key 是否对应同一个请求内容�?/p>
 * <p>3. 使用 Redisson 分布式锁保护同一个幂�?key 的读写过程�?/p>
 * <p>4. 第一次请求写�?PROCESSING，成功后改为 SUCCESS 并保存响应体�?/p>
 * <p>5. 重复请求读取已有状态，避免重复执行业务逻辑�?/p>
 */
@Service
public class RedisIdempotencyService implements IdempotencyService {

    /**
     * 幂等记录默认保留 24 小时。实际项目可以根据业务重试窗口调整�?     */
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final DistributedLockService distributedLockService;

    public RedisIdempotencyService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper,
            DistributedLockService distributedLockService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.distributedLockService = distributedLockService;
    }

    @Override
    public IdempotencyCheckResult checkAndStart(String bizType, String key, String requestHash) {
        // 第一步：构造业务隔离后�?Redis key，避免不同业务之�?key 冲突�?
        String redisKey = redisKey(bizType, key);

        // 第二步：对当前幂�?key 加分布式锁，保证“检查记�?+ 写入占位记录”是串行的�?
        return distributedLockService.execute("lock:" + redisKey, 3, 10, TimeUnit.SECONDS, () -> {
            // 第三步：读取已有幂等记录�?
            IdempotencyRecord record = readRecord(redisKey);
            if (record == null) {
                // 第四步：记录不存在，说明这是第一次请求。先写入 PROCESSING 占位�?
                IdempotencyRecord newRecord = new IdempotencyRecord();
                newRecord.setRequestHash(requestHash);
                newRecord.setStatus(IdempotencyStatus.PROCESSING);
                newRecord.setCreatedAt(LocalDateTime.now());
                newRecord.setUpdatedAt(LocalDateTime.now());
                writeRecord(redisKey, newRecord);
                return IdempotencyCheckResult.started();
            }
            // 第五步：同一�?Idempotency-Key 不允许对应不同请求内容，否则调用方使用方式有问题�?
            if (!requestHash.equals(record.getRequestHash())) {
                throw new BizException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
            }
            // 第六步：记录已存在且请求内容一致，说明这是重复请求，返回已有状态�?
            return IdempotencyCheckResult.repeated(record.getStatus(), record.getResponseBody());
        });
    }

    @Override
    public void markSuccess(String bizType, String key, String responseBody) {
        updateStatus(bizType, key, IdempotencyStatus.SUCCESS, responseBody, null);
    }

    @Override
    public void markFailed(String bizType, String key, String errorMessage) {
        updateStatus(bizType, key, IdempotencyStatus.FAILED, null, errorMessage);
    }

    private void updateStatus(String bizType, String key, IdempotencyStatus status, String responseBody, String errorMessage) {
        String redisKey = redisKey(bizType, key);
        // 状态更新也加锁，避免重复请求读取状态时与 SUCCESS/FAILED 更新交叉。
        distributedLockService.execute("lock:" + redisKey, 3, 10, TimeUnit.SECONDS, () -> {
            IdempotencyRecord record = readRecord(redisKey);
            if (record == null) {
                // 理论上 markSuccess/markFailed 前应该已经有 PROCESSING 记录。
                // 这里兜底创建，避免异常情况下状态完全丢失。
                record = new IdempotencyRecord();
                record.setCreatedAt(LocalDateTime.now());
            }
            record.setStatus(status);
            record.setResponseBody(responseBody);
            record.setErrorMessage(errorMessage);
            record.setUpdatedAt(LocalDateTime.now());
            writeRecord(redisKey, record);
        });
    }

    private String redisKey(String bizType, String key) {
        // Redis key 格式固定，便于排查和按业务类型批量检索�?
        return "idempotency:" + bizType + ":" + key;
    }

    private IdempotencyRecord readRecord(String redisKey) {
        // Redis 中保�?JSON 字符串，读取后反序列化为 IdempotencyRecord�?
        String json = stringRedisTemplate.opsForValue().get(redisKey);
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, IdempotencyRecord.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to read idempotency record", exception);
        }
    }

    private void writeRecord(String redisKey, IdempotencyRecord record) {
        try {
            // 每次写入都刷新 TTL，保证从最近一次状态更新开始继续保留一段时间。
            stringRedisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(record), DEFAULT_TTL);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to write idempotency record", exception);
        }
    }
}
