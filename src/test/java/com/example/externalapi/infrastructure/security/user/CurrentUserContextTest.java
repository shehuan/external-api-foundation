package com.example.externalapi.infrastructure.security.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * 验证当前用户上下文的 ThreadLocal 写入和清理。
 */
class CurrentUserContextTest {

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void shouldStoreAndClearCurrentUserInThreadLocal() {
        LoginUser loginUser = new LoginUser(10001L, "admin", List.of("ADMIN"));

        CurrentUserContext.set(loginUser);

        assertEquals(loginUser, CurrentUserContext.get());
        assertEquals(10001L, CurrentUserContext.getUserId());

        CurrentUserContext.clear();

        assertNull(CurrentUserContext.get());
        assertNull(CurrentUserContext.getUserId());
    }
}
