package com.technogise.leave_management_system.controller;
import com.technogise.leave_management_system.dto.UpdateUserRoleRequest;
import com.technogise.leave_management_system.dto.UserResponse;
import com.technogise.leave_management_system.response.SuccessResponse;
import com.technogise.leave_management_system.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

@Slf4j
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<SuccessResponse<Page<UserResponse>>> getAllUsers(
            Pageable pageable
    ) {
        log.info("GET /api/users called, page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<UserResponse> usersPage = userService.getAllUsers(pageable);

        log.debug("Returning {} users", usersPage.getTotalElements());

        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.success("Users retrieved successfully", usersPage));
    }

    @PatchMapping("/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SuccessResponse<Boolean>> updateUserRole(
            @Valid @RequestBody UpdateUserRoleRequest request) {
        boolean result = userService.updateRole(request);
        return ResponseEntity.ok(
                new SuccessResponse<>(true, "Role updated successfully", result)
        );
    }
}
