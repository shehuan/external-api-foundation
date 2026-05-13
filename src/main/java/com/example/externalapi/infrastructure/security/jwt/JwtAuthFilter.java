package com.example.externalapi.infrastructure.security.jwt;

import com.example.externalapi.common.error.ErrorCode;
import com.example.externalapi.infrastructure.logging.RequestLogAttributes;
import com.example.externalapi.infrastructure.security.user.CurrentUserContext;
import com.example.externalapi.infrastructure.security.user.LoginUser;
import com.example.externalapi.infrastructure.web.JsonResponseWriter;
import com.example.externalapi.infrastructure.web.PathMatchUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT 鉴权过滤器。
 *
 * <p>该过滤器负责判断当前接口是否需要 token，并在需要时校验 Authorization 请求头。</p>
 * <p>校验成功后会将 LoginUser 写入 CurrentUserContext，供 Controller、Service 和审计字段自动填充使用。</p>
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProperties jwtProperties;
    private final JwtTokenProvider jwtTokenProvider;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtProperties jwtProperties, JwtTokenProvider jwtTokenProvider, ObjectMapper objectMapper) {
        this.jwtProperties = jwtProperties;
        this.jwtTokenProvider = jwtTokenProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // 第一步：根据配置判断当前路径是否需要 JWT 鉴权。不需要鉴权的接口直接放行。
        if (!requiresAuth(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 第二步：读取 Authorization 请求头。当前约定格式为：Authorization: Bearer <token>。
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            JsonResponseWriter.write(response, objectMapper, ErrorCode.TOKEN_MISSING);
            return;
        }

        // 第三步：截取 Bearer 后面的 token 内容。
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            JsonResponseWriter.write(response, objectMapper, ErrorCode.TOKEN_MISSING);
            return;
        }

        try {
            // 第四步：解析 token，校验签名、issuer、过期时间，并还原登录用户。
            LoginUser loginUser = jwtTokenProvider.parseLoginUser(token);

            // 第五步：把当前用户写入 ThreadLocal 上下文和请求属性，后续业务代码和外层日志都可以读取。
            CurrentUserContext.set(loginUser);
            request.setAttribute(RequestLogAttributes.USER_ID, loginUser.userId());

            // 第六步：继续执行后续 Filter 和 Controller。
            filterChain.doFilter(request, response);
        } catch (ExpiredJwtException exception) {
            // token 已过期，返回明确错误码，方便调用方刷新或重新登录。
            JsonResponseWriter.write(response, objectMapper, ErrorCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException exception) {
            // 签名错误、issuer 不一致、token 格式错误等都归为 TOKEN_INVALID。
            JsonResponseWriter.write(response, objectMapper, ErrorCode.TOKEN_INVALID);
        } finally {
            // 第七步：必须清理 ThreadLocal。Tomcat 线程会复用，不清理会导致用户串号。
            CurrentUserContext.clear();
        }
    }

    private boolean requiresAuth(HttpServletRequest request) {
        // JWT 总开关关闭或 OPTIONS 预检请求，不做鉴权。
        if (!jwtProperties.isEnabled() || PathMatchUtils.isOptions(request)) {
            return false;
        }
        String path = request.getRequestURI();
        // exclude 优先级高于 include。登录、文档、健康检查等接口通常放在 exclude。
        if (PathMatchUtils.matchesAny(jwtProperties.getExcludePaths(), path)) {
            return false;
        }
        // 最后判断 include。当前配置通常为 /**，表示除 exclude 外全部需要鉴权。
        return PathMatchUtils.matchesAny(jwtProperties.getIncludePaths(), path);
    }
}
