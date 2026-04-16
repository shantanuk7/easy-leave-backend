package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.UpdateUserRoleRequest;
import com.technogise.leave_management_system.dto.UserResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAllByOrderByNameAsc(pageable)
                .map(u -> new UserResponse(
                        u.getId(),
                        u.getEmail(),
                        u.getName(),
                        u.getRole()
                ));
    }

    public boolean updateRole(UUID adminId, UpdateUserRoleRequest request) {

        if (adminId.equals(request.getEmployeeId())) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "You cannot change your own role");
        }

        User user = userRepository.findById(request.getEmployeeId())
                .orElseThrow(() ->
                        new HttpException(HttpStatus.NOT_FOUND,
                                "User not found with id: " + request.getEmployeeId()));

        if (user.getRole().equals(request.getRole())) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "User already has role: " + request.getRole());
        }

        user.setRole(request.getRole());
        userRepository.save(user);

        return true;
    }
}
