package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserByUserId(UUID id) {
        return userRepository.findById(id).orElseThrow(
                () -> new HttpException(HttpStatus.NOT_FOUND, "User not found with id: " + id));
    }
}

