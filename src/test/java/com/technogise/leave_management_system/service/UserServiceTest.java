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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
    void shouldReturnPagedUserResponses() {
        User employee = new User();
        employee.setId(UUID.randomUUID());
        employee.setName("PRIYANSH");
        employee.setEmail("priyansh@technogise.com");
        employee.setRole(UserRole.EMPLOYEE);
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setName("RAJ");
        admin.setEmail("raj@technogise.com");
        admin.setRole(UserRole.ADMIN);

        List<User> users = List.of(employee, admin);
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());

        when(userRepository.findAllByOrderByNameAsc(any(Pageable.class)))
                .thenReturn(userPage);

        Page<UserResponse> result = userService.getAllUsers(pageable);
        assertEquals(2, result.getContent().size());
        UserResponse first = result.getContent().getFirst();
        assertEquals(employee.getId(), first.getId());
        assertEquals(employee.getEmail(), first.getEmail());
        UserResponse second = result.getContent().get(1);
        assertEquals(admin.getId(), second.getId());
        assertEquals(admin.getEmail(), second.getEmail());
    }
}

