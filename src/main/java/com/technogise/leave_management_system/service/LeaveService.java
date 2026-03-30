package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.CreateLeaveRequest;
import com.technogise.leave_management_system.dto.CreateLeaveResponse;
import com.technogise.leave_management_system.dto.LeaveResponse;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.technogise.leave_management_system.enums.ScopeType.ORGANIZATION;
import static com.technogise.leave_management_system.enums.ScopeType.SELF;
import static com.technogise.leave_management_system.enums.StatusType.COMPLETED;
import static com.technogise.leave_management_system.enums.StatusType.ONGOING;
import static com.technogise.leave_management_system.enums.StatusType.UPCOMING;

@Service
public class LeaveService {

    private final UserRepository userRepository;
    private final LeaveRepository leaveRepository;
    private final UserService userService;
    private final LeaveCategoryService leaveCategoryService;

    public LeaveService(LeaveRepository leaveRepository,
                        UserService userService,
                        LeaveCategoryService leaveCategoryService,
                        UserRepository userRepository) {
        this.leaveRepository = leaveRepository;
        this.userService = userService;
        this.userRepository = userRepository;
        this.leaveCategoryService = leaveCategoryService;
    }

    public User findUserById(UUID id) {
        return userRepository.findById(id).orElseThrow(
                () -> new HttpException(HttpStatus.NOT_FOUND, "User not found with id: " + id));
    }

    public List<Leave> filterLeavesByScope(String scope, User user) {
        if (scope.equalsIgnoreCase(SELF.toString())) {
            return leaveRepository.findAllByUserId(user.getId(), Sort.by(Sort.Direction.DESC, "createdAt"));
        } else if (scope.equalsIgnoreCase(ORGANIZATION.toString())) {
            if (user.getRole().equals(UserRole.MANAGER)) {
                return leaveRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
            }
            throw new HttpException(HttpStatus.FORBIDDEN, "Not Allowed to access this resource");
        }
        throw new HttpException(HttpStatus.BAD_REQUEST, "Invalid scope query parameter");
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
        throw new HttpException(HttpStatus.BAD_REQUEST, "Invalid status query parameter");
    }

    public List<LeaveResponse> getAllLeaves(UUID userId, String scope, String status) {
        User user = findUserById(userId);
        List<Leave> leaveList = filterLeavesByScope(scope, user);

        if (status != null && !status.isBlank()) {
            leaveList = filterLeavesByStatus(status, leaveList);
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



    public boolean isWeekendDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    @Transactional
    public List<CreateLeaveResponse> applyLeave(CreateLeaveRequest request, UUID userId) {
        User user = userService.getUserByUserId(userId);
        LeaveCategory category = leaveCategoryService.getLeaveCategoryById(request.getLeaveCategoryId());

        List<LocalDate> workingDaysOnly = request.getDates().stream()
                .filter(date -> !isWeekendDay(date))
                .toList();

        if (workingDaysOnly.isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "Cannot apply for leave on weekends.");
        }

        List<Leave> existingLeaves = leaveRepository.findAllByUserId(userId, Sort.unsorted());
        Set<LocalDate> alreadyTakenDates = existingLeaves.stream()
                .map(Leave::getDate)
                .collect(Collectors.toSet());

        List<LocalDate> newDatesToApply = workingDaysOnly.stream()
                .filter(date -> !alreadyTakenDates.contains(date))
                .toList();

        if (newDatesToApply.isEmpty()) {
            throw new HttpException(HttpStatus.CONFLICT, "All selected working days have already been applied for.");
        }

        List<CreateLeaveResponse> responses = new ArrayList<>();
        for (LocalDate date : newDatesToApply) {
            Leave leave = new Leave();
            leave.setDate(date);
            leave.setUser(user);
            leave.setLeaveCategory(category);
            leave.setDescription(request.getDescription());
            leave.setStartTime(request.getStartTime());
            leave.setDuration(request.getDuration());

            leaveRepository.save(leave);

            responses.add(new CreateLeaveResponse(
                    leave.getId(),
                    leave.getDate(),
                    category.getName(),
                    leave.getDuration(),
                    leave.getStartTime(),
                    leave.getDescription()
            ));
        }

        return responses;
    }
}
