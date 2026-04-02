package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.UserResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User findOrCreateUser(String email, String name) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> createUser(email, name));
    }

    public User getUserByUserId(UUID id) {
        return userRepository.findById(id).orElseThrow(
                () -> new HttpException(HttpStatus.NOT_FOUND, "User not found with id: " + id));
    }

    private User createUser(String email, String name) {
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setName(name);
        newUser.setRole(UserRole.EMPLOYEE);
        return userRepository.save(newUser);
    }
    private void validateAccess(User user) {
        if (UserRole.EMPLOYEE.equals(user.getRole())) {
            throw new HttpException(HttpStatus.FORBIDDEN, "Access denied");
        }
    }

    public List<UserResponse> getAllUsers(UUID requestingUserId) {
        User requestingUser = getUserByUserId(requestingUserId);
        validateAccess(requestingUser);

        List<User> allUsers = userRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));

        return allUsers.stream()
                .map(u -> new UserResponse(
                        u.getId(),
                        u.getEmail(),
                        u.getName(),
                        u.getRole()
                )).toList();
    }
}

