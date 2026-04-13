package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.EmployeeLeavesRecordResponse;
import com.technogise.leave_management_system.dto.UserResponse;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.LeaveCategoryRepository;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final LeaveCategoryRepository leaveCategoryRepository;
    private final LeaveRepository leaveRepository;

    public UserService(
            UserRepository userRepository,
            LeaveCategoryRepository leaveCategoryRepository,
            LeaveRepository leaveRepository
    ) {
        this.userRepository = userRepository;
        this.leaveCategoryRepository = leaveCategoryRepository;
        this.leaveRepository = leaveRepository;
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

    public List<EmployeeLeavesRecordResponse> getEmployeeLeavesRecordByYear(UUID userId, int year) {
        getUserByUserId(userId);

        List<LeaveCategory> leaveCategories = leaveCategoryRepository.findAll();
        List<EmployeeLeavesRecordResponse> leavesRecord = new ArrayList<>();

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        for (LeaveCategory category : leaveCategories) {
            long leavesTaken = leaveRepository.countByUserIdAndLeaveCategoryIdAndDateBetween(userId, category.getId(), startDate, endDate);

            if (leavesTaken > 0) {
                leavesRecord.add(EmployeeLeavesRecordResponse
                        .builder()
                        .leaveId(category.getId())
                        .leaveType(category.getName())
                        .leavesTaken(leavesTaken)
                        .totalLeavesAvailable(category.getAllocatedDays())
                        .leavesRemaining(category.getAllocatedDays() - leavesTaken)
                        .build()
                );
            }
        }

        return leavesRecord;
    }
}
