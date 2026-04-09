package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.AuthUserResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.response.SuccessResponse;
import com.technogise.leave_management_system.service.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

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
}
