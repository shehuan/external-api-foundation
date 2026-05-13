package com.example.externalapi.infrastructure.logging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.externalapi.common.response.ApiResponse;
import com.example.externalapi.infrastructure.crypto.config.CryptoProperties;
import com.example.externalapi.infrastructure.crypto.key.DefaultCryptoKeyResolver;
import com.example.externalapi.infrastructure.crypto.provider.NoopPayloadCryptoProvider;
import com.example.externalapi.infrastructure.crypto.provider.PayloadCryptoProviderRegistry;
import com.example.externalapi.infrastructure.crypto.web.CryptoFilter;
import com.example.externalapi.infrastructure.security.jwt.JwtAuthFilter;
import com.example.externalapi.infrastructure.security.jwt.JwtProperties;
import com.example.externalapi.infrastructure.security.jwt.JwtTokenProvider;
import com.example.externalapi.infrastructure.security.user.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestResponseLogFilterTest {

    private static final String SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldLogPlainBodiesWhenCryptoPlainLogSwitchEnabled() throws Exception {
        CapturedLogs capturedLogs = captureAccessLogs();
        try {
            executeRequest(true);
        } finally {
            capturedLogs.stop();
        }

        String logs = capturedLogs.messages();
        assertTrue(logs.contains("REQ  {\"amount\":99.99}"));
        assertTrue(logs.contains("RESP {\"code\":0,\"message\":\"success\",\"data\":{\"orderId\":1},"));
        assertFalse(logs.contains("REQ  {\"data\":\"{\\\"amount\\\":99.99}\"}"));
        assertFalse(logs.contains("\"data\":{\"data\":\"{\\\"orderId\\\":1}\"}"));
    }

    @Test
    void shouldLogFinalCipherBodiesWhenCryptoPlainLogSwitchDisabled() throws Exception {
        CapturedLogs capturedLogs = captureAccessLogs();
        try {
            executeRequest(false);
        } finally {
            capturedLogs.stop();
        }

        String logs = capturedLogs.messages();
        assertTrue(logs.contains("REQ  {\"data\":\"{\\\"amount\\\":99.99}\"}"));
        assertTrue(logs.contains("\"data\":{\"data\":\"{\\\"orderId\\\":1}\"}"));
        assertFalse(logs.contains("REQ  {\"amount\":99.99}"));
    }

    @Test
    void shouldLogUserIdAfterJwtContextIsCleared() throws Exception {
        CapturedLogs capturedLogs = captureAccessLogs();
        try {
            executeRequest(true);
        } finally {
            capturedLogs.stop();
        }

        assertTrue(capturedLogs.messages().contains("userId=10001 code=0"));
    }

    private void executeRequest(boolean plainCryptoBodyEnabled) throws Exception {
        RequestResponseLogFilter logFilter = new RequestResponseLogFilter(logProperties(plainCryptoBodyEnabled),
                objectMapper);
        CryptoFilter cryptoFilter = new CryptoFilter(cryptoProperties(), objectMapper,
                new DefaultCryptoKeyResolver(cryptoProperties()),
                new PayloadCryptoProviderRegistry(List.of(new NoopPayloadCryptoProvider())));
        JwtAuthFilter jwtAuthFilter = new JwtAuthFilter(jwtProperties(), jwtTokenProvider(), objectMapper);

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/example/orders");
        request.setContentType(MediaType.APPLICATION_JSON_VALUE);
        request.addHeader(CryptoFilter.APP_ID_HEADER, "partner-a");
        request.addHeader(CryptoFilter.CRYPTO_ALGORITHM_HEADER, NoopPayloadCryptoProvider.ALGORITHM);
        request.addHeader("Authorization", "Bearer " + jwtTokenProvider()
                .generateToken(new LoginUser(10001L, "test-user", List.of("USER"))));
        request.setContent("{\"data\":\"{\\\"amount\\\":99.99}\"}".getBytes(StandardCharsets.UTF_8));

        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = nestedChain(List.of(logFilter, cryptoFilter, jwtAuthFilter), this::writeSuccessResponse);

        chain.doFilter(request, response);
    }

    private LogProperties logProperties(boolean plainCryptoBodyEnabled) {
        LogProperties properties = new LogProperties();
        properties.setBeginEnabled(true);
        properties.setRequestBodyEnabled(true);
        properties.setResponseBodyEnabled(true);
        properties.setPlainCryptoBodyEnabled(plainCryptoBodyEnabled);
        return properties;
    }

    private CryptoProperties cryptoProperties() {
        CryptoProperties properties = new CryptoProperties();
        properties.setEnabled(true);
        return properties;
    }

    private JwtProperties jwtProperties() {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("demo-service");
        properties.setSecret(SECRET);
        properties.setAccessTokenExpireMinutes(10);
        properties.setIncludePaths(List.of("/**"));
        return properties;
    }

    private JwtTokenProvider jwtTokenProvider() {
        return new JwtTokenProvider(jwtProperties());
    }

    private void writeSuccessResponse(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.success(Map.of("orderId", 1)));
    }

    private FilterChain nestedChain(List<Filter> filters, TerminalHandler terminalHandler) {
        return new FilterChain() {
            private int index;

            @Override
            public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response)
                    throws IOException, ServletException {
                if (index < filters.size()) {
                    filters.get(index++).doFilter(request, response, this);
                    return;
                }
                terminalHandler.handle((HttpServletRequest) request, (HttpServletResponse) response);
            }
        };
    }

    private CapturedLogs captureAccessLogs() {
        Logger logger = (Logger) LoggerFactory.getLogger("HTTP_ACCESS");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO);
        return new CapturedLogs(logger, appender);
    }

    @FunctionalInterface
    private interface TerminalHandler {
        void handle(HttpServletRequest request, HttpServletResponse response) throws IOException;
    }

    private record CapturedLogs(Logger logger, ListAppender<ILoggingEvent> appender) {

        String messages() {
            StringBuilder builder = new StringBuilder();
            for (ILoggingEvent event : appender.list) {
                builder.append(event.getFormattedMessage()).append('\n');
            }
            return builder.toString();
        }

        void stop() {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
