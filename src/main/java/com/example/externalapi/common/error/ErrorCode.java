package com.example.externalapi.common.error;

public enum ErrorCode {

    SUCCESS(0, "success", 200),

    BAD_REQUEST(400000, "Bad request", 400),
    PARAM_MISSING(400001, "Required parameter is missing", 400),
    PARAM_INVALID(400002, "Invalid parameter", 400),
    PARAM_TYPE_MISMATCH(400003, "Parameter type mismatch", 400),
    BODY_MISSING(400004, "Request body is required", 400),
    ENCRYPTED_BODY_INVALID(400010, "Encrypted request body is invalid", 400),

    UNAUTHORIZED(401000, "Unauthorized", 401),
    TOKEN_MISSING(401001, "Access token is missing", 401),
    TOKEN_INVALID(401002, "Access token is invalid", 401),
    TOKEN_EXPIRED(401003, "Access token has expired", 401),
    SIGN_MISSING(401010, "Request signature is missing", 401),
    SIGN_INVALID(401011, "Request signature is invalid", 401),
    TIMESTAMP_INVALID(401012, "Request timestamp is invalid", 401),
    REQUEST_EXPIRED(401013, "Request has expired", 401),
    NONCE_MISSING(401014, "Request nonce is missing", 401),
    APP_ID_INVALID(401015, "AppId is invalid", 401),

    FORBIDDEN(403000, "Forbidden", 403),
    NOT_FOUND(404000, "Resource not found", 404),
    METHOD_NOT_ALLOWED(405000, "Method not allowed", 405),
    CONFLICT(409000, "Request conflict", 409),
    REPLAY_REQUEST(409001, "Request is duplicated or already processed", 409),
    IDEMPOTENCY_KEY_CONFLICT(409002, "Idempotency key conflicts with request content", 409),

    TOO_MANY_REQUESTS(429000, "Too many requests", 429),
    REPEAT_SUBMIT(429001, "Do not submit repeatedly", 429),

    INTERNAL_ERROR(500000, "Internal server error", 500),
    DECRYPT_FAILED(500010, "Request decrypt failed", 500),
    ENCRYPT_FAILED(500011, "Response encrypt failed", 500),
    CRYPTO_KEY_NOT_FOUND(500012, "Crypto key is not found", 500),
    CRYPTO_PROVIDER_NOT_FOUND(500013, "Crypto provider is not found", 500),
    EXTERNAL_SERVICE_ERROR(500020, "External service call failed", 500),

    USER_NOT_FOUND(100001, "User not found", 404),
    USER_DISABLED(100002, "User is disabled", 403),
    USERNAME_EXISTS(100003, "Username already exists", 409);

    private final int code;
    private final String message;
    private final int httpStatus;

    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
