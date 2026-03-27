package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.LeaveResponse;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.ApplicationException;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.technogise.leave_management_system.enums.ScopeType.SELF;
import static com.technogise.leave_management_system.enums.ScopeType.TEAM;
import static com.technogise.leave_management_system.enums.StatusType.*;

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
                () -> new ApplicationException(HttpStatus.NOT_FOUND, "User not found with id: " + id));
    }

    public List<Leave> filterLeavesByScope(String scope, User user) {
        if (scope.equalsIgnoreCase(SELF.toString())) {
            return leaveRepository.findAllByUserId(user.getId(), Sort.by(Sort.Direction.DESC, "createdAt"));
        } else if (scope.equalsIgnoreCase(TEAM.toString())) {
            if (user.getRole().equals(UserRole.MANAGER)) {
                return leaveRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            }
            throw new ApplicationException(HttpStatus.FORBIDDEN,"Not Allowed to access this resource");
        }
        throw new ApplicationException(HttpStatus.BAD_REQUEST,"Invalid scope query parameter");
    }

    public List<Leave> filterLeavesByStatus(String status, List<Leave> leaveList) {
        if (status.equalsIgnoreCase(UPCOMING.toString())) {
            return leaveList.stream()
                    .filter(leave -> leave.getDate().isAfter(LocalDate.now()))
                    .toList();
        } else if (status.equalsIgnoreCase(COMPLETED.toString())) {
            return leaveList.stream()
                    .filter(leave -> leave.getDate().isBefore(LocalDate.now()))
                    .toList();
        } else if (status.equalsIgnoreCase(ONGOING.toString())) {
            return leaveList.stream()
                    .filter(leave -> leave.getDate().equals(LocalDate.now()))
                    .toList();
        }
        throw new ApplicationException(HttpStatus.BAD_REQUEST,"Invalid status query parameter");
    }
    public List<LeaveResponse> getAllLeaves(UUID userId,String scope, String status) {
        User user = findUserById(userId);
        List<Leave> leaveList = filterLeavesByScope(scope,user);

        if (status != null && !status.isBlank()) {
            leaveList = filterLeavesByStatus(status,leaveList);
        }
        return leaveList.stream().map(leave -> new LeaveResponse(
                leave.getId(),
                leave.getDate(),
                leave.getUser().getName(),
                leave.getLeaveCategory().getName(),
                leave.getDuration(),
                leave.getStartTime(),
                leave.getUpdatedAt(),
                leave.getDescription()
        )).toList();
    }
}
