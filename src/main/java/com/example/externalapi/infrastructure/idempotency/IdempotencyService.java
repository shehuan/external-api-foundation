package com.example.externalapi.infrastructure.idempotency;

/**
 * 幂等服务接口�? *
 * <p>幂等的目标不是“拦截重复点击”，而是保证同一个业务请求被重复发送时，不会重复产生业务副作用�?/p>
 * <p>典型场景：创建订单、支付回调、发放权益、扣减库存、外部系统重试�?/p>
 */
public interface IdempotencyService {

    /**
     * 检查幂等记录并尝试开始处理�?     *
     * <p>调用方应该在真正执行业务逻辑之前调用该方法�?/p>
     *
     * @param bizType 业务类型，例�?CREATE_ORDER，用于隔离不同业务的幂等 key
     * @param key 调用方传入的幂等键，通常来自 Idempotency-Key 请求�?     * @param requestHash 当前请求内容的摘要，用于判断“同一�?key 是否被不同请求体复用�?     * @return 幂等检查结果；firstRequest=true 表示当前请求可以执行业务
     */
    IdempotencyCheckResult checkAndStart(String bizType, String key, String requestHash);

    /**
     * 标记业务处理成功�?     *
     * <p>业务成功后保存响应体，后续相�?key + 相同请求体的重复请求可以直接返回第一次结果�?/p>
     */
    void markSuccess(String bizType, String key, String responseBody);

    /**
     * 标记业务处理失败�?     *
     * <p>当前实现不会自动重试失败请求，而是记录失败状态，避免失败场景下产生不确定的重复执行�?/p>
     */
    void markFailed(String bizType, String key, String errorMessage);
}
