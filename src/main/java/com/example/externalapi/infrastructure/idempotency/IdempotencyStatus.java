package com.example.externalapi.infrastructure.idempotency;

/**
 * 幂等处理状态�? */
public enum IdempotencyStatus {
    /**
     * 第一次请求已经占位，业务逻辑正在执行�?     */
    PROCESSING,

    /**
     * 业务执行成功，响应结果已经保存�?     */
    SUCCESS,

    /**
     * 业务执行失败，当前实现不自动重试�?     */
    FAILED
}
