package com.example.externalapi.infrastructure.logging;

public final class RequestLogAttributes {

    public static final String PLAIN_REQUEST_BODY = RequestLogAttributes.class.getName() + ".PLAIN_REQUEST_BODY";
    public static final String PLAIN_RESPONSE_BODY = RequestLogAttributes.class.getName() + ".PLAIN_RESPONSE_BODY";
    public static final String USER_ID = RequestLogAttributes.class.getName() + ".USER_ID";

    private RequestLogAttributes() {
    }
}
