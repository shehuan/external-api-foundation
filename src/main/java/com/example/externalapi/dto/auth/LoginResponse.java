package com.example.externalapi.dto.auth;

public record LoginResponse(
        String accessToken,
        long expiresInSeconds
) {
}
