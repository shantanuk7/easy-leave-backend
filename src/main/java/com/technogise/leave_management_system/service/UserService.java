package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.UpdateUserRoleRequest;
import com.technogise.leave_management_system.dto.EmployeeLeavesRecordResponse;
import com.technogise.leave_management_system.dto.UserResponse;
import com.technogise.leave_management_system.entity.AnnualLeave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.AnnualLeaveRepository;
import com.technogise.leave_management_system.repository.LeaveCategoryRepository;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final LeaveCategoryRepository leaveCategoryRepository;
    private final LeaveRepository leaveRepository;
    private final AnnualLeaveRepository annualLeaveRepository;

    public UserService(
            UserRepository userRepository,
            LeaveCategoryRepository leaveCategoryRepository,
            LeaveRepository leaveRepository,
            AnnualLeaveRepository annualLeaveRepository
    ) {
        this.userRepository = userRepository;
        this.leaveCategoryRepository = leaveCategoryRepository;
        this.leaveRepository = leaveRepository;
        this.annualLeaveRepository = annualLeaveRepository;
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

    @Transactional
    public void updateRole(UUID adminId, UpdateUserRoleRequest request) {
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
    }

    public List<EmployeeLeavesRecordResponse> getEmployeeLeavesRecordByYear(UUID userId, Integer year) {
        getUserByUserId(userId);

        int requestedYear = (year != null) ? year : LocalDate.now(ZoneId.of("Asia/Kolkata")).getYear();
        Optional<AnnualLeave> annualLeave =
                annualLeaveRepository.findByUserIdAndYear(userId, String.valueOf(requestedYear));

        List<LeaveCategory> leaveCategories = leaveCategoryRepository.findAll();
        List<EmployeeLeavesRecordResponse> leavesRecord = new ArrayList<>();

        LocalDate startDate = LocalDate.of(requestedYear, 1, 1);
        LocalDate endDate = LocalDate.of(requestedYear, 12, 31);

        for (LeaveCategory category : leaveCategories) {
            long leavesTaken = leaveRepository.
                    countByUserIdAndLeaveCategoryIdAndDateBetweenAndDeletedAtIsNull(userId, category.getId(), startDate, endDate);

            double totalLeaves = category.getName().equalsIgnoreCase("Annual Leave")
                    ? annualLeave.map(AnnualLeave::getTotal).orElse(0.0) : category.getAllocatedDays();

            if (leavesTaken > 0) {
                leavesRecord.add(EmployeeLeavesRecordResponse
                        .builder()
                        .leaveId(category.getId())
                        .leaveType(category.getName())
                        .leavesTaken(leavesTaken)
                        .totalLeavesAvailable(totalLeaves)
                        .leavesRemaining(totalLeaves - leavesTaken)
                        .build()
                );
            }
        }

        return leavesRecord;
    }
}
