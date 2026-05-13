package com.example.externalapi.controller;

import com.example.externalapi.common.response.ApiResponse;
import com.example.externalapi.infrastructure.repeat.NoRepeatSubmit;
import com.example.externalapi.infrastructure.security.user.CurrentUserContext;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PingController {

    @Operation(summary = "Protected ping API")
    @GetMapping("/ping")
    public ApiResponse<Map<String, Object>> ping() {
        return ApiResponse.success(Map.of(
                "pong", true,
                "userId", CurrentUserContext.getUserId()
        ));
    }

    @Operation(summary = "Example repeat-submit protected API")
    @NoRepeatSubmit(seconds = 5)
    @PostMapping("/repeat-submit-demo")
    public ApiResponse<Map<String, Object>> repeatSubmitDemo(@RequestBody Map<String, Object> request) {
        return ApiResponse.success(Map.of("received", request));
    }
}
