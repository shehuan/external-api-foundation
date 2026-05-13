package com.example.externalapi.infrastructure.security.user;

import java.util.List;

/**
 * 当前登录用户的轻量模型。
 *
 * <p>该对象来自 JWT claims，只保存接口鉴权和审计常用字段，不建议放手机号、证件号等敏感信息。</p>
 */
public record LoginUser(
        /**
         * 用户 ID，通常来自 JWT subject。
         */
        Long userId,
        /**
         * 用户名，用于日志、展示或简单业务判断。
         */
        String username,
        /**
         * 用户角色列表，后续可扩展为权限判断。
         */
        List<String> roles
) {
}
