package com.example.externalapi.infrastructure.idempotency;

/**
 * 幂等检查结果�? *
 * @param firstRequest true 表示第一次请求，调用方可以继续执行业务逻辑
 * @param status 当前幂等记录状�? * @param responseBody 已成功请求保存的响应体；重复请求可以复用该结�? */
public record IdempotencyCheckResult(
        boolean firstRequest,
        IdempotencyStatus status,
        String responseBody
) {

    /**
     * 表示当前请求是第一次进入，幂等记录已经初始化为 PROCESSING�?     */
    public static IdempotencyCheckResult started() {
        return new IdempotencyCheckResult(true, IdempotencyStatus.PROCESSING, null);
    }

    /**
     * 表示当前请求是重复请求，返回 Redis 中已有记录的状态和响应体�?     */
    public static IdempotencyCheckResult repeated(IdempotencyStatus status, String responseBody) {
        return new IdempotencyCheckResult(false, status, responseBody);
    }
}
