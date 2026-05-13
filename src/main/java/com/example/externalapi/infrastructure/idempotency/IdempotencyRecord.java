package com.example.externalapi.infrastructure.idempotency;

import java.time.LocalDateTime;

/**
 * Redis 中保存的幂等记录。
 *
 * <p>一条记录对应一个业务类型 + 一个 Idempotency-Key。</p>
 * <p>记录中保存 requestHash 是为了防止调用方复用同一个幂等 key 发送不同业务内容。</p>
 */
public class IdempotencyRecord {

    /**
     * 请求内容摘要。相同幂等 key 下，requestHash 必须一致。
     */
    private String requestHash;

    /**
     * 当前处理状态：处理中、成功、失败。
     */
    private IdempotencyStatus status;

    /**
     * 首次成功处理后的响应体，用于重复请求直接复用结果。
     */
    private String responseBody;

    /**
     * 失败原因，仅用于内部排查或后续扩展。
     */
    private String errorMessage;

    /**
     * 记录创建时间。
     */
    private LocalDateTime createdAt;

    /**
     * 记录最后更新时间。
     */
    private LocalDateTime updatedAt;

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public void setStatus(IdempotencyStatus status) {
        this.status = status;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
