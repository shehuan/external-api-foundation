package com.example.externalapi.infrastructure.repeat;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 防重复提交注解。
 *
 * <p>用于短时间内拦截同一个用户或同一个 IP 对同一接口、同一请求体的重复提交。</p>
 * <p>它适合防止按钮连点、短时间重复调用，不等同于业务幂等。</p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NoRepeatSubmit {

    /**
     * 防重复提交窗口，单位秒。小于等于 0 时使用全局默认值。
     */
    long seconds() default -1;

    /**
     * 重复提交时返回的错误消息。
     */
    String message() default "Do not submit repeatedly";
}
