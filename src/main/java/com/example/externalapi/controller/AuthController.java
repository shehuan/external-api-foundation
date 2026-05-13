package com.example.externalapi.controller;

import com.example.externalapi.common.response.ApiResponse;
import com.example.externalapi.dto.auth.LoginRequest;
import com.example.externalapi.dto.auth.LoginResponse;
import com.example.externalapi.infrastructure.security.jwt.JwtProperties;
import com.example.externalapi.infrastructure.security.jwt.JwtTokenProvider;
import com.example.externalapi.infrastructure.security.user.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;

    public AuthController(JwtTokenProvider jwtTokenProvider, JwtProperties jwtProperties) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.jwtProperties = jwtProperties;
    }

    @Operation(summary = "Login and return JWT token")
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginUser loginUser = new LoginUser(10001L, request.username(), List.of("USER"));
        String token = jwtTokenProvider.generateToken(loginUser);
        return ApiResponse.success(new LoginResponse(token, jwtProperties.getAccessTokenExpireMinutes() * 60));
    }
}
