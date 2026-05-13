package com.example.externalapi.infrastructure.web;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.util.AntPathMatcher;

public final class PathMatchUtils {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private PathMatchUtils() {
    }

    public static boolean matchesAny(List<String> patterns, String path) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        return patterns.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

    public static boolean isOptions(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }
}
