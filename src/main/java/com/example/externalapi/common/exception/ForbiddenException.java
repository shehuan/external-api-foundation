package com.example.externalapi.common.exception;

import com.example.externalapi.common.error.ErrorCode;

public class ForbiddenException extends BizException {

    public ForbiddenException() {
        super(ErrorCode.FORBIDDEN);
    }

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
}
