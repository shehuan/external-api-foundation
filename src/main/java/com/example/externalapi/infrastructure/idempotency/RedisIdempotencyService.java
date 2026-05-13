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
 * 基于 Redis 的幂等服务实现。
 *
 * <p>核心思路：</p>
 * <p>1. 使用 bizType + Idempotency-Key 组成 Redis key。</p>
 * <p>2. 使用 requestHash 判断同一个 key 是否对应同一份请求内容。</p>
 * <p>3. 使用 Redisson 分布式锁保护同一个幂等 key 的读写过程。</p>
 * <p>4. 第一次请求写入 PROCESSING，成功后改为 SUCCESS 并保存响应体。</p>
 * <p>5. 重复请求读取已有状态，避免重复执行业务逻辑。</p>
 */
@Service
public class RedisIdempotencyService implements IdempotencyService {

    /**
     * 幂等记录默认保留 24 小时。实际项目可以根据业务重试窗口调整。
     */
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
        // 构造带业务类型的 Redis key，避免不同业务复用同一个外部幂等键时互相冲突。
        String redisKey = redisKey(bizType, key);

        // 同一个幂等 key 的“读取现有记录 + 写入 PROCESSING 占位”必须串行执行。
        return distributedLockService.execute("lock:" + redisKey, 3, 10, TimeUnit.SECONDS, () -> {
            IdempotencyRecord record = readRecord(redisKey);
            if (record == null) {
                // 第一次请求先占位，再让业务代码真正执行。
                IdempotencyRecord newRecord = new IdempotencyRecord();
                newRecord.setRequestHash(requestHash);
                newRecord.setStatus(IdempotencyStatus.PROCESSING);
                newRecord.setCreatedAt(LocalDateTime.now());
                newRecord.setUpdatedAt(LocalDateTime.now());
                writeRecord(redisKey, newRecord);
                return IdempotencyCheckResult.started();
            }
            // 同一个 Idempotency-Key 对应不同请求体，说明调用方复用了不该复用的幂等键。
            if (!requestHash.equals(record.getRequestHash())) {
                throw new BizException(ErrorCode.IDEMPOTENCY_KEY_CONFLICT);
            }
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
        // 状态更新也加锁，避免重复请求读到更新中的半成品记录。
        distributedLockService.execute("lock:" + redisKey, 3, 10, TimeUnit.SECONDS, () -> {
            IdempotencyRecord record = readRecord(redisKey);
            if (record == null) {
                // 正常情况下 checkAndStart 已经创建 PROCESSING；这里兜底，避免最终状态完全丢失。
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
        // 固定 key 格式便于排查问题，也便于按业务类型批量清理。
        return "idempotency:" + bizType + ":" + key;
    }

    private IdempotencyRecord readRecord(String redisKey) {
        // Redis 中保存 JSON 字符串，记录结构扩展时不需要改变 key 设计。
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
            // 每次写入都刷新 TTL，保证从最近一次状态更新后继续保留完整窗口。
            stringRedisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(record), DEFAULT_TTL);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to write idempotency record", exception);
        }
    }
}
