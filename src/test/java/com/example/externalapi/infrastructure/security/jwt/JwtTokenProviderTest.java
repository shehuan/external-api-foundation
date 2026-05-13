package com.example.externalapi.infrastructure.security.jwt;

import com.example.externalapi.infrastructure.security.user.LoginUser;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    @Test
    void generateTestToken() {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("demo-service");
        properties.setSecret("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");
        properties.setAccessTokenExpireMinutes(10);

        JwtTokenProvider provider = new JwtTokenProvider(properties);
        String token = provider.generateToken(new LoginUser(10001L, "test-user", List.of("USER")));

        System.out.println("Bearer " + token);
    }
}
