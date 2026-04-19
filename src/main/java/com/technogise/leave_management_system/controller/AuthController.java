package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.AuthUserResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.response.SuccessResponse;
import com.technogise.leave_management_system.service.AuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    @Value("${app.cookie.secure}")
    private boolean cookieSecure;
    @Value("${app.cookie.same.site}")
    private String cookieSameSite;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<SuccessResponse<AuthUserResponse>> getAuthenticatedUser(
            @AuthenticationPrincipal User user
    ) {
        log.info("GET /api/auth/me called by userId={}", user.getId());

        UUID userId = user.getId();
        AuthUserResponse authUser = authService.getAuthenticatedUser(userId);

        log.debug("Returning auth info for userId={}, role={}", user.getId(), authUser.getRole());

        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.success("User retrieved successfully", authUser));
    }

    @PostMapping("/logout")
    public ResponseEntity<SuccessResponse<Void>> logout(HttpServletResponse response) {
        log.info("POST /api/auth/logout called");

        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", cookieSameSite);

        response.addCookie(cookie);

        log.info("JWT cookie cleared successfully");

        return ResponseEntity.ok(SuccessResponse.success("Logout successful", null));
    }
}
