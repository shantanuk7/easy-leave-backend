package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    User createValidUser() {
        return new User();
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
}
