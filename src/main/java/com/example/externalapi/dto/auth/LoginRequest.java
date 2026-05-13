package com.example.externalapi.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "username is required")
        @Size(max = 50, message = "username length must not exceed 50")
        String username,

        @NotBlank(message = "password is required")
        @Size(min = 6, max = 64, message = "password length must be 6-64")
        String password
) {
}
