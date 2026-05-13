package com.example.externalapi.common.exception;

import com.example.externalapi.common.error.ErrorCode;

public class UnauthorizedException extends BizException {

    public UnauthorizedException(ErrorCode errorCode) {
        super(errorCode);
    }

    public UnauthorizedException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
