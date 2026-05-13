package com.example.externalapi.infrastructure.security.user;

/**
 * 当前登录用户上下文。
 *
 * <p>使用 ThreadLocal 保存一次请求内的登录用户，避免在 Controller/Service 方法之间层层传 userId。</p>
 * <p>该上下文由 JwtAuthFilter 写入，并且必须在 finally 中清理。</p>
 */
public final class CurrentUserContext {

    /**
     * 每个请求线程独立保存 LoginUser。
     */
    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(LoginUser loginUser) {
        // JWT 校验成功后写入当前登录用户。
        HOLDER.set(loginUser);
    }

    public static LoginUser get() {
        // 没有登录态或当前接口未鉴权时返回 null。
        return HOLDER.get();
    }

    public static Long getUserId() {
        // 常用便捷方法，审计字段自动填充、日志打印可以直接使用。
        LoginUser loginUser = HOLDER.get();
        return loginUser == null ? null : loginUser.userId();
    }

    public static void clear() {
        // 必须调用 remove，而不是 set(null)，避免线程池复用造成内存泄漏或用户串号。
        HOLDER.remove();
    }
}
