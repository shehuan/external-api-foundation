package com.example.externalapi.infrastructure.web;

import com.example.externalapi.common.error.ErrorCode;
import com.example.externalapi.common.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;

public final class JsonResponseWriter {

    private JsonResponseWriter() {
    }

    public static void write(HttpServletResponse response, ObjectMapper objectMapper, ErrorCode errorCode) throws IOException {
        write(response, objectMapper, errorCode, errorCode.getMessage());
    }

    public static void write(HttpServletResponse response, ObjectMapper objectMapper, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.getHttpStatus());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.failure(errorCode, message));
    }
}
