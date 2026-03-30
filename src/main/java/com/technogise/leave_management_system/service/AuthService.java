package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.AuthUserResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.exception.ApplicationException;
import com.technogise.leave_management_system.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthUserResponse getAuthenticatedUser(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new ApplicationException(HttpStatus.NOT_FOUND, "User not found"));

        return new AuthUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
