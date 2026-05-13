package com.example.externalapi.infrastructure.repeat;

import com.example.externalapi.common.error.ErrorCode;
import com.example.externalapi.common.exception.BizException;
import com.example.externalapi.infrastructure.logging.CachedBodyHttpServletRequest;
import com.example.externalapi.infrastructure.security.replay.SignatureUtils;
import com.example.externalapi.infrastructure.security.user.CurrentUserContext;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 防重复提交切面�? *
 * <p>拦截标注�?{@link NoRepeatSubmit} 的方法，在业务方法执行前�?Redis SET NX 做短期占位�?/p>
 * <p>如果占位失败，说明同一个身份、同一个接口、同一个请求体在时间窗口内已经提交过，直接返回错误�?/p>
 */
@Aspect
@Component
public class NoRepeatSubmitAspect {

    private final RepeatSubmitProperties repeatSubmitProperties;
    private final StringRedisTemplate stringRedisTemplate;

    public NoRepeatSubmitAspect(RepeatSubmitProperties repeatSubmitProperties, StringRedisTemplate stringRedisTemplate) {
        this.repeatSubmitProperties = repeatSubmitProperties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Around("@annotation(noRepeatSubmit)")
    public Object around(ProceedingJoinPoint joinPoint, NoRepeatSubmit noRepeatSubmit) throws Throwable {
        // 第一步：总开关关闭时，不做任何拦截�?
        if (!repeatSubmitProperties.isEnabled()) {
            return joinPoint.proceed();
        }

        // 第二步：获取当前 HTTP 请求。非 Web 调用场景下直接放行。
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }

        // 第三步：计算防重复窗口。注解优先，没有配置则使用全局默认秒数�?
        long seconds = noRepeatSubmit.seconds() > 0 ? noRepeatSubmit.seconds() : repeatSubmitProperties.getDefaultSeconds();

        // 第四步：构�?Redis key。key 中包含用�?IP、请求方法、URI、请求体摘要�?
        String key = buildKey(request);

        // 第五步：使用 Redis SET NX EX。第一次提交成功写入，重复提交会写入失败�?
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(seconds));
        if (!Boolean.TRUE.equals(success)) {
            throw new BizException(ErrorCode.REPEAT_SUBMIT, noRepeatSubmit.message());
        }

        // 第六步：占位成功后执行业务方法�?
        return joinPoint.proceed();
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }

    private String buildKey(HttpServletRequest request) {
        // 优先使用登录用户 ID；未登录接口则退化为客户�?IP�?
        Long userId = CurrentUserContext.getUserId();
        String identity = userId == null ? getClientIp(request) : String.valueOf(userId);

        // 请求体参与 key 计算，避免同一接口不同参数被错误拦截。
        byte[] body = request instanceof CachedBodyHttpServletRequest cachedRequest
                ? cachedRequest.getCachedBody()
                : new byte[0];
        String bodyHash = SignatureUtils.sha256Hex(body);
        return "repeat:submit:" + identity + ":" + request.getMethod() + ":" + request.getRequestURI() + ":" + bodyHash;
    }

    private String getClientIp(HttpServletRequest request) {
        // 优先读取代理转发头，适配 Nginx、网关等部署方式�?
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp;
    }
}
