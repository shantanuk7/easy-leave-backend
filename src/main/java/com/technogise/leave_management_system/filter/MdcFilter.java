package com.technogise.leave_management_system.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@Order(1)
public class MdcFilter extends OncePerRequestFilter {

    private static final Map<String, String> PATH_TO_LAYER_MAP = Map.of(
            "/oauth2", "AUTH",
            "/api/auth", "AUTH",
            "/api/leaves", "LEAVE",
            "/api/users", "USER",
            "/api/annual-leaves", "ANNUAL-LEAVE",
            "/api/leave-categories", "LEAVE-CATEGORY"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Find the first matching prefix or default to "APP"
        String layer = PATH_TO_LAYER_MAP.entrySet().stream()
                .filter(entry -> path.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("APP");

        try {
            MDC.put("layer", layer);
            filterChain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
