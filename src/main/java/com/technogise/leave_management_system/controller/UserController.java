package com.technogise.leave_management_system.controller;
import com.technogise.leave_management_system.dto.UserResponse;
import com.technogise.leave_management_system.response.SuccessResponse;
import com.technogise.leave_management_system.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<SuccessResponse<List<UserResponse>>> getAllUsers(
            Pageable pageable
    ) {
        Page<UserResponse> usersPage = userService.getAllUsers(pageable);
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.success("Users retrieved successfully", usersPage.getContent()));
    }
}
