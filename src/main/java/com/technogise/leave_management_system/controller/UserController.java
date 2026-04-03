package com.technogise.leave_management_system.controller;
import com.technogise.leave_management_system.dto.UserResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.response.SuccessResponse;
import com.technogise.leave_management_system.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<SuccessResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> usersList = userService.getAllUsers();
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.success("Users retrieved successfully", usersList));
    }
}
