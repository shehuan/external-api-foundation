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
 * 防重复提交切面。
 *
 * <p>拦截标注 {@link NoRepeatSubmit} 的方法，在业务方法执行前用 Redis SET NX 做短期占位。</p>
 * <p>如果占位失败，说明同一个身份、同一个接口、同一个请求体在时间窗口内已经提交过，直接返回错误。</p>
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
        // 运行时读取总开关，便于通过配置关闭整个防重复提交模块。
        if (!repeatSubmitProperties.isEnabled()) {
            return joinPoint.proceed();
        }

        // 非 Web 调用不在该防护范围内，直接放行。
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return joinPoint.proceed();
        }

        long seconds = noRepeatSubmit.seconds() > 0 ? noRepeatSubmit.seconds() : repeatSubmitProperties.getDefaultSeconds();
        String key = buildKey(request);

        // SET NX EX：第一次提交写入成功，窗口期内相同提交会写入失败。
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(seconds));
        if (!Boolean.TRUE.equals(success)) {
            throw new BizException(ErrorCode.REPEAT_SUBMIT, noRepeatSubmit.message());
        }

        return joinPoint.proceed();
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }

    private String buildKey(HttpServletRequest request) {
        // 优先使用登录用户 ID；公开接口退化为客户端 IP。
        Long userId = CurrentUserContext.getUserId();
        String identity = userId == null ? getClientIp(request) : String.valueOf(userId);

        // key 中包含请求体摘要，避免同一接口的不同参数被互相拦截。
        byte[] body = request instanceof CachedBodyHttpServletRequest cachedRequest
                ? cachedRequest.getCachedBody()
                : new byte[0];
        String bodyHash = SignatureUtils.sha256Hex(body);
        return "repeat:submit:" + identity + ":" + request.getMethod() + ":" + request.getRequestURI() + ":" + bodyHash;
    }

    private String getClientIp(HttpServletRequest request) {
        // 优先读取代理转发头，适配 Nginx、网关等部署方式。
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp;
    }
}
