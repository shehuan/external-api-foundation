package com.example.externalapi.common.response;

import com.example.externalapi.common.error.ErrorCode;
import com.example.externalapi.infrastructure.logging.TraceIdUtils;

public record ApiResponse<T>(
        int code,
        String message,
        T data,
        String traceId
) {

    public static ApiResponse<Void> success() {
        return success(null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data, TraceIdUtils.getTraceId());
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode) {
        return failure(errorCode, errorCode.getMessage());
    }

    public static ApiResponse<Void> failure(ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.getCode(), message, null, TraceIdUtils.getTraceId());
    }
}
