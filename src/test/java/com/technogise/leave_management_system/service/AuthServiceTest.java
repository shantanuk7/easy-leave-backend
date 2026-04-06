package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.AuthUserResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void shouldReturnUserIfUserIsAuthenticated() {
        // Given
        UUID userId = UUID.randomUUID();

        User user = new User();
        user.setId(userId);
        user.setName("Raj");
        user.setEmail("raj@gmail.com");
        user.setRole(UserRole.EMPLOYEE);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When
        AuthUserResponse result = authService.getAuthenticatedUser(userId);

        // Then
        assertEquals(userId, result.getId());
        assertEquals("Raj", result.getName());
        assertEquals("raj@gmail.com", result.getEmail());
        assertEquals("EMPLOYEE", result.getRole());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        HttpException exception = assertThrows(
                HttpException.class,
                () -> authService.getAuthenticatedUser(userId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("User not found", exception.getMessage());
    }
}
