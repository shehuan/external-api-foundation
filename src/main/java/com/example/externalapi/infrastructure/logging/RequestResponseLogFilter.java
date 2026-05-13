package com.example.externalapi.infrastructure.logging;

import com.example.externalapi.infrastructure.security.user.CurrentUserContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Order(Ordered.HIGHEST_PRECEDENCE)
@Component
public class RequestResponseLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger("HTTP_ACCESS");

    private final LogProperties logProperties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public RequestResponseLogFilter(LogProperties logProperties, ObjectMapper objectMapper) {
        this.logProperties = logProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = TraceIdUtils.resolveTraceId(request.getHeader(TraceIdUtils.TRACE_ID_HEADER));
        MDC.put(TraceIdUtils.MDC_TRACE_ID, traceId);
        response.setHeader(TraceIdUtils.TRACE_ID_HEADER, traceId);

        long start = System.currentTimeMillis();
        HttpServletRequest requestToUse = wrapRequestIfNecessary(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
        String pathWithQuery = buildPathWithQuery(request);
        String ip = getClientIp(request);

        if (logProperties.isBeginEnabled() && shouldLog(request)) {
            log.info("HTTP BEGIN {} {} traceId={} ip={} userId=-", request.getMethod(), pathWithQuery, traceId, ip);
        }

        try {
            filterChain.doFilter(requestToUse, responseWrapper);
        } finally {
            try {
                if (shouldLog(request)) {
                    logEnd(requestToUse, responseWrapper, start, pathWithQuery, ip);
                }
            } finally {
                responseWrapper.copyBodyToResponse();
                MDC.remove(TraceIdUtils.MDC_TRACE_ID);
            }
        }
    }

    private HttpServletRequest wrapRequestIfNecessary(HttpServletRequest request) throws IOException {
        if (request instanceof CachedBodyHttpServletRequest || isMultipart(request)) {
            return request;
        }
        return new CachedBodyHttpServletRequest(request);
    }

    private boolean shouldLog(HttpServletRequest request) {
        String path = request.getRequestURI();
        return logProperties.getExcludePaths().stream().noneMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private boolean isMultipart(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith(MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    private void logEnd(HttpServletRequest request, ContentCachingResponseWrapper responseWrapper, long start,
            String pathWithQuery, String ip) {
        long costMs = System.currentTimeMillis() - start;
        int status = responseWrapper.getStatus();
        String responseBody = getResponseBody(request, responseWrapper);
        Integer code = parseCode(responseBody);
        String userId = resolveUserId(request);

        log.info("HTTP END   {} {} -> {} {}ms traceId={} ip={} userId={} code={}",
                request.getMethod(), pathWithQuery, status, costMs, TraceIdUtils.getTraceId(), ip, userId, code);

        if (logProperties.isRequestBodyEnabled()) {
            log.info("REQ  {}", getRequestBody(request));
        }
        if (logProperties.isResponseBodyEnabled()) {
            log.info("RESP {}", formatBody(responseBody));
        }
    }

    private String getRequestBody(HttpServletRequest request) {
        String plainBody = getPlainBodyAttribute(request, RequestLogAttributes.PLAIN_REQUEST_BODY);
        if (plainBody != null) {
            return formatBody(plainBody);
        }
        if (!(request instanceof CachedBodyHttpServletRequest cachedRequest)) {
            return "-";
        }
        return formatBody(cachedRequest.getCachedBodyAsString());
    }

    private String getResponseBody(HttpServletRequest request, ContentCachingResponseWrapper responseWrapper) {
        String plainBody = getPlainBodyAttribute(request, RequestLogAttributes.PLAIN_RESPONSE_BODY);
        if (plainBody != null) {
            return plainBody;
        }
        byte[] bytes = responseWrapper.getContentAsByteArray();
        if (bytes.length == 0) {
            return "";
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String getPlainBodyAttribute(HttpServletRequest request, String name) {
        if (!logProperties.isPlainCryptoBodyEnabled()) {
            return null;
        }
        Object value = request.getAttribute(name);
        return value instanceof String body ? body : null;
    }

    private String resolveUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(RequestLogAttributes.USER_ID);
        if (userId != null) {
            return userId.toString();
        }
        Long contextUserId = CurrentUserContext.getUserId();
        return contextUserId == null ? "-" : contextUserId.toString();
    }

    private String formatBody(String body) {
        String masked = MaskUtils.maskJson(objectMapper, body);
        if (masked.length() <= logProperties.getMaxBodyLength()) {
            return masked;
        }
        return masked.substring(0, logProperties.getMaxBodyLength()) + "...[truncated,total=" + masked.length() + "]";
    }

    private Integer parseCode(String responseBody) {
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            JsonNode code = node.get("code");
            return code == null || !code.isNumber() ? null : code.asInt();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String buildPathWithQuery(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null || query.isBlank() ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.isBlank() ? request.getRemoteAddr() : realIp;
    }
}
