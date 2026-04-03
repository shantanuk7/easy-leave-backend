package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.*;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.enums.WeekendDay;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.LeaveRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.Arrays;
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

    private final LeaveRepository leaveRepository;
    private final UserService userService;
    private final LeaveCategoryService leaveCategoryService;

    public LeaveService(LeaveRepository leaveRepository,
                        UserService userService,
                        LeaveCategoryService leaveCategoryService) {
        this.leaveRepository = leaveRepository;
        this.userService = userService;
        this.leaveCategoryService = leaveCategoryService;
    }

    public List<Leave> filterLeavesByScope(String scope, User user) {
        if (scope.equalsIgnoreCase(SELF.toString())) {
            return leaveRepository.findAllByUserId(user.getId(), Sort.by(Sort.Direction.DESC, "date"));
        } else if (scope.equalsIgnoreCase(ORGANIZATION.toString())) {
            if (user.getRole().equals(UserRole.MANAGER)) {
                return leaveRepository.findAll(Sort.by(Sort.Direction.DESC, "date"));
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
        User user = userService.getUserByUserId(userId);
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

    public boolean isValidLeaveDate(LocalDate date) {
        LocalDate today = LocalDate.now();

        if (date.getYear() != today.getYear()) {
            return false;
        }

        if (date.isBefore(today)) {
            return date.getMonth().equals(today.getMonth());
        }

        return true;
    }

    public boolean isWeekendDay(LocalDate date) {
        return Arrays.stream(WeekendDay.values())
                .anyMatch(weekend -> weekend.getDayOfWeek() == date.getDayOfWeek());
    }

    @Transactional
    public List<CreateLeaveResponse> applyLeave(CreateLeaveRequest request, UUID userId) {
        User user = userService.getUserByUserId(userId);
        LeaveCategory category = leaveCategoryService.getLeaveCategoryById(request.getLeaveCategoryId());
        List<LocalDate> validDates = request.getDates().stream()
                .filter(this::isValidLeaveDate).toList();
        if (validDates.isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Dates must be within the current month for past dates, or within the current year for future dates.");
        }
        List<LocalDate> workingDaysOnly = validDates.stream()
                .filter(date -> !isWeekendDay(date)).toList();

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
            throw new HttpException(HttpStatus.CONFLICT,
                    "All selected working days have already been applied for.");
        }
        List<Leave> leavesToSave = newDatesToApply.stream()
                .map(date -> {
                    Leave leave = new Leave();
                    leave.setDate(date);
                    leave.setUser(user);
                    leave.setLeaveCategory(category);
                    leave.setDescription(request.getDescription());
                    leave.setStartTime(request.getStartTime());
                    leave.setDuration(request.getDuration());
                    return leave;
                }).toList();

        List<Leave> savedLeaves = leaveRepository.saveAll(leavesToSave);
        return savedLeaves.stream()
                .map(leave -> new CreateLeaveResponse(
                        leave.getId(),
                        leave.getDate(),
                        category.getName(),
                        leave.getDuration(),
                        leave.getStartTime(),
                        leave.getDescription()
                )).toList();
    }

    public LeaveResponse getLeaveById(UUID leaveId, UUID userId) {
        User user = userService.getUserByUserId(userId);
        Leave leave = leaveRepository.findById(leaveId).orElseThrow(
                () -> new HttpException(HttpStatus.NOT_FOUND, "Leave not found with id: " + leaveId));

        if (!leave.getUser().getId().equals(userId) && !user.getRole().equals(UserRole.MANAGER)) {
            throw new HttpException(HttpStatus.FORBIDDEN, "Not Allowed to access this resource");
        }

        return new LeaveResponse(
                leave.getId(),
                leave.getDate(),
                leave.getUser().getName(),
                leave.getLeaveCategory().getName(),
                leave.getDuration(),
                leave.getStartTime(),
                leave.getUpdatedAt(),
                leave.getDescription()
        );
    }

    public boolean isValidLeaveOwner(Leave leave, UUID userId) {
        return leave.getUser().getId().equals(userId);
    }

    @Transactional
    public UpdateLeaveResponse updateLeave(UUID leaveId, UpdateLeaveRequest request, UUID userId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND,
                        "Leave not found with id: " + leaveId));

        if (!isValidLeaveOwner(leave, userId)) {
            throw new HttpException(HttpStatus.FORBIDDEN,
                    "Not allowed to update this leave");
        }

        if (!isValidLeaveDate(leave.getDate())) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Cannot edit a leave that is no longer within the updatable date range");
        }

        if (!isValidLeaveDate(request.getDate())) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "New date must be within the current month for past dates, or within the current year for future dates");
        }

        if (isWeekendDay(request.getDate())) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Cannot update leave to a weekend date");
        }

        return null;
    }
}
