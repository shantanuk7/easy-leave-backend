package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.UserResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private final String email = "rakshit@technogise.com";
    private final String name = "Rakshit Saxena";
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    User createValidUser() {
        return new User();
    }

    @Test
    void shouldReturnExistingUserWhenUserExists() {

        User existingUser = new User();
        existingUser.setEmail(email);
        existingUser.setName(name);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));

        User result = userService.findOrCreateUser(email, name);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
    }

    @Test
    void shouldCreateAndReturnNewUserWhenUserDoesNotExist() {

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        User savedUser = new User();
        savedUser.setEmail(email);
        savedUser.setRole(UserRole.EMPLOYEE);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.findOrCreateUser(email, name);

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals(UserRole.EMPLOYEE, result.getRole());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserIdDoesNotExist() {

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(HttpException.class,
                () -> userService.getUserByUserId(userId));
    }

    @Test
    void shouldReturnUserWhenUserIdDoesExist() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));

        User user = userService.getUserByUserId(userId);

        assertInstanceOf(User.class, user);
    }

    @Test
    void shouldThrowForbiddenExceptionWhenUserIsEmployee() {
        User requestingUser = new User();
        requestingUser.setId(userId);
        requestingUser.setRole(UserRole.EMPLOYEE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(requestingUser));
        HttpException exception = assertThrows(HttpException.class,
                () -> userService.getAllUsers(userId));
        assertEquals("Access denied", exception.getMessage());
    }

    @Test
    void shouldReturnUsersListWhenRequestingUserIsAdmin() {
        User requestingUser = new User();
        requestingUser.setId(userId);
        requestingUser.setRole(UserRole.ADMIN);
        User employee = new User();
        employee.setId(UUID.randomUUID());
        employee.setRole(UserRole.EMPLOYEE);
        when(userRepository.findById(userId)).thenReturn(Optional.of(requestingUser));
        when(userRepository.findAll(Sort.by(Sort.Direction.ASC, "name")))
                .thenReturn(List.of(requestingUser, employee));
        List<UserResponse> responses = userService.getAllUsers(userId);
        assertEquals(2, responses.size());
        assertEquals(requestingUser.getRole(), responses.get(0).getRole());
        assertEquals(employee.getRole(), responses.get(1).getRole());
    }
}