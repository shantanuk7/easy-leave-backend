package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.LeaveResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.exception.InvalidQueryParameterException;
import com.technogise.leave_management_system.exception.ResourceNotFoundException;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class LeaveService {
    private final UserRepository userRepository;
    private final LeaveRepository leaveRepository;
    public LeaveService(UserRepository userRepository, LeaveRepository leaveRepository) {
        this.userRepository = userRepository;
        this.leaveRepository = leaveRepository;
    }
    public User findUserById(UUID id) {
        return userRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("NOT_FOUND", "User not found with id: " + id));
    }
    public List<LeaveResponse> getAllLeaves(UUID userId, String status, UUID requestedEmployeeId) {
        User user = findUserById(userId);
        List<LeaveResponse> leaveList = leaveRepository
                .findAllByUserId(userId, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(leave -> new LeaveResponse(
                        leave.getId(),
                        leave.getDate(),
                        user.getName(),
                        leave.getLeaveCategory().getName(),
                        leave.getDuration(),
                        leave.getStartTime(),
                        leave.getUpdatedAt(),
                        leave.getDescription()
                )).toList();
        if(status != null) {
            if ("upcoming".equalsIgnoreCase(status)) {
                leaveList = leaveList.stream()
                        .filter(leave -> leave.getDate() != null && leave.getDate().after(new Date()))
                        .toList();
            } else if ("completed".equalsIgnoreCase(status)) {
                leaveList = leaveList.stream()
                        .filter(leave -> leave.getDate() != null && leave.getDate().before(new Date()))
                        .toList();
            }
            else {
                throw new InvalidQueryParameterException("BAD_REQUEST","invalid status query parameter");
            }
        }
        return leaveList;
    }
}