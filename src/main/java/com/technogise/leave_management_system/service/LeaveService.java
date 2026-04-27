package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.constants.LeaveConstants;
import com.technogise.leave_management_system.dto.CreateLeaveRequest;
import com.technogise.leave_management_system.dto.CreateLeaveResponse;
import com.technogise.leave_management_system.dto.LeaveResponse;
import com.technogise.leave_management_system.dto.UpdateLeaveRequest;
import com.technogise.leave_management_system.dto.UpdateLeaveResponse;
import com.technogise.leave_management_system.entity.Holiday;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.HolidayType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.enums.WeekendDay;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.handler.LeaveIntegrationHandler;
import com.technogise.leave_management_system.repository.LeaveRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

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
    private final AnnualLeaveService annualLeaveService;
    private final LeaveIntegrationHandler leaveIntegrationHandler;
    private final HolidayService holidayService;

    @Value("${leave.optional-holiday.max-days}")
    private int maxOptionalHolidayDays;

    public LeaveService(LeaveRepository leaveRepository,
                        UserService userService,
                        LeaveCategoryService leaveCategoryService,
                        AnnualLeaveService annualLeaveService,
                        LeaveIntegrationHandler leaveIntegrationHandler,
                        HolidayService holidayService
    ) {
        this.leaveRepository = leaveRepository;
        this.userService = userService;
        this.leaveCategoryService = leaveCategoryService;
        this.annualLeaveService = annualLeaveService;
        this.leaveIntegrationHandler = leaveIntegrationHandler;
        this.holidayService = holidayService;
    }

    public List<Leave> filterLeavesByScope(String scope, User user) {
        if (scope.equalsIgnoreCase(SELF.toString())) {
            return leaveRepository.findAllByUserIdAndDeletedAtNull(user.getId(), Sort.by(Sort.Direction.DESC, "date"));
        } else if (scope.equalsIgnoreCase(ORGANIZATION.toString())) {
            if (user.getRole().equals(UserRole.MANAGER)) {
                return leaveRepository.findAllByDeletedAtIsNull(Sort.by(Sort.Direction.DESC, "date"));
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

    private LeaveResponse mapToLeaveResponse(Leave leave) {
        String type = getLeaveType(leave);
        return new LeaveResponse(
                leave.getId(),
                leave.getDate(),
                leave.getUser().getName(),
                type,
                leave.getDuration(),
                leave.getStartTime(),
                leave.getUpdatedAt(),
                leave.getDescription()
        );
    }

    private List<Leave> getLeavesByScopeAndStatus(User user, String scope, String status) {
        List<Leave> leaveList = filterLeavesByScope(scope, user);

        if (status != null && !status.isBlank()) {
            return filterLeavesByStatus(status, leaveList);
        }

        return leaveList;
    }
    private List<Leave> getLeavesByEmployeeAndYear(User user, UUID empId, Integer year) {
        if (!user.getRole().equals(UserRole.MANAGER)) {
            throw new HttpException(HttpStatus.FORBIDDEN,
                    "Not allowed to access this resource");
        }

        userService.getUserByUserId(empId);

        int targetYear = (year != null) ? year : LocalDate.now(ZoneId.of("Asia/Kolkata")).getYear();
        LocalDate startDate = LocalDate.of(targetYear, 1, 1);
        LocalDate endDate = LocalDate.of(targetYear, 12, 31);

        return leaveRepository.findAllByUserIdAndDateBetweenAndDeletedAtIsNull(
                empId, startDate, endDate,
                Sort.by(Sort.Direction.DESC, "date"));
    }

    public List<LeaveResponse> getAllLeaves(UUID userId, String scope, String status, UUID empId, Integer year) {
        User user = userService.getUserByUserId(userId);

        if (empId != null && !scope.equalsIgnoreCase(ORGANIZATION.toString())) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "empId can only be used when scope is ORGANIZATION");
        }

        List<Leave> leaveList = (empId != null)
                ? getLeavesByEmployeeAndYear(user, empId, year)
                : getLeavesByScopeAndStatus(user, scope, status);

        return leaveList.stream().map(this::mapToLeaveResponse).toList();
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

    private boolean isFixedHoliday(LocalDate date) {
        List<Holiday> fixedHolidays = holidayService.getHolidaysByType(HolidayType.FIXED);

        return fixedHolidays.stream().anyMatch(holiday -> holiday.getDate().equals(date));
    }

    private void validateMutualExclusiveness(boolean hasHoliday, boolean hasCategory) {
        if (hasHoliday && hasCategory) {
            throw new HttpException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot apply for a leave with both fields provided. Provide either holidayId or leaveCategoryId."
            );
        }
        if (!hasHoliday && !hasCategory) {
            throw new HttpException(
                    HttpStatus.BAD_REQUEST,
                    "At least one of the two fields must be provided holiday_id or category_id."
            );
        }
    }

    private void validateOptionalHolidaysCount(User user) {
        int currentYear = LocalDate.now(ZoneId.of("Asia/Kolkata")).getYear();
        LocalDate startDate = LocalDate.of(currentYear, 1, 1);
        LocalDate endDate = LocalDate.of(currentYear, 12, 31);

        long optionalHolidaysCount = leaveRepository.countByUserIdAndHolidayIsNotNullAndDateBetweenAndDeletedAtIsNull(
                user.getId(),
                startDate,
                endDate
        );

        if (optionalHolidaysCount >= maxOptionalHolidayDays) {
            throw new HttpException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot apply more than allocated days for optional holidays"
            );
        }
    }

    private String getLeaveType(Leave leave) {
        if (leave.getLeaveCategory() != null) {
            return leave.getLeaveCategory().getName();
        } else {
            return leave.getHoliday().getType().toString() + " HOLIDAY";
        }
    }

    @Transactional
    public List<CreateLeaveResponse> applyLeave(CreateLeaveRequest request, UUID userId) {

        boolean hasHoliday = request.getHolidayId() != null;
        boolean hasCategory = request.getLeaveCategoryId() != null;
        validateMutualExclusiveness(hasHoliday, hasCategory);

        User user = userService.getUserByUserId(userId);
        LeaveCategory category = hasCategory
                ? leaveCategoryService.getLeaveCategoryById(request.getLeaveCategoryId())
                : null;

        Holiday holiday = hasHoliday
                ? holidayService.getHolidayById(request.getHolidayId())
                : null;

        if (hasHoliday) {
            validateOptionalHolidaysCount(user);
        }

        List<LocalDate> workingDates =
                filterValidWorkingDates(request.getDates());

        List<LocalDate> newDates =
                filterNonOverlappingLeaveDates(userId, workingDates);

        List<Leave> leavesToSave = newDates.stream()
                .map(date -> {
                    Leave leave = leaveRepository.findByUserIdAndDate(userId, date)
                            .orElse(new Leave());
                    leave.setDate(date);
                    leave.setUser(user);
                    leave.setLeaveCategory(category);
                    leave.setHoliday(holiday);
                    leave.setDescription(request.getDescription());
                    leave.setStartTime(request.getStartTime());
                    leave.setDuration(request.getDuration());
                    leave.setDeletedAt(null);
                    return leave;
                }).toList();

        List<Leave> savedLeaves = leaveRepository.saveAll(leavesToSave);

        if (category != null && category.getName().equals(LeaveConstants.ANNUAL_LEAVE)) {
            annualLeaveService.syncOnLeaveCreated(user, request.getDuration(), newDates.size(), LocalDate.now().getYear());
        }
        leaveIntegrationHandler.handleLeaves(savedLeaves);

        String leaveTypeName = hasCategory ? category.getName() : holiday.getName();

        return savedLeaves.stream()
                .map(leave -> new CreateLeaveResponse(
                        leave.getId(),
                        leave.getDate(),
                        leaveTypeName,
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

        String type = getLeaveType(leave);

        return new LeaveResponse(leave.getId(), leave.getDate(), leave.getUser().getName(), type,
                leave.getDuration(),
                leave.getStartTime(),
                leave.getUpdatedAt(),
                leave.getDescription()
        );
    }

    private List<LocalDate> filterValidWorkingDates(List<LocalDate> requestedDates) {

        List<LocalDate> validDates = requestedDates.stream()
                .filter(this::isValidLeaveDate)
                .toList();

        if (validDates.isEmpty()) {
            throw new HttpException(
                    HttpStatus.BAD_REQUEST,
                    "Dates must be within current month for past dates, or current year for future dates"
            );
        }

        List<LocalDate> workingDays = validDates.stream()
                .filter(date -> !isWeekendDay(date))
                .filter(date -> !isFixedHoliday(date))
                .toList();

        if (workingDays.isEmpty()) {
            throw new HttpException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot apply leave on weekends or fixed holidays"
            );
        }

        return workingDays;
    }

    private List<LocalDate> filterNonOverlappingLeaveDates(UUID userId, List<LocalDate> dates) {

        Set<LocalDate> existingDates = leaveRepository
                .findAllByUserIdAndDeletedAtNull(userId, Sort.unsorted())
                .stream()
                .map(Leave::getDate)
                .collect(Collectors.toSet());

        List<LocalDate> newDates = dates.stream()
                .filter(date -> !existingDates.contains(date))
                .toList();

        if (newDates.isEmpty()) {
            throw new HttpException(
                    HttpStatus.CONFLICT,
                    "All selected working days already applied"
            );
        }

        return newDates;
    }

    public boolean isValidLeaveOwner(Leave leave, UUID userId) {
        return leave.getUser().getId().equals(userId);
    }

    @Transactional
    public UpdateLeaveResponse updateLeave(UUID leaveId, UpdateLeaveRequest request, UUID userId) {
        validateUpdateRequestNotEmpty(request);
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "Leave not found with id: " + leaveId));

        validateLeaveOwnership(leave, userId, "Not allowed to update this leave");
        validateExistingLeaveDate(leave.getDate());

        DurationType oldDuration = leave.getDuration();
        String oldCategoryName = leave.getLeaveCategory().getName();

        if (request.getDate() != null) {
            validateNewLeaveDate(request.getDate());
            validateNewLeaveDateIsNotWeekend(request.getDate());
            validateNoDateConflict(userId, leaveId, request.getDate());
            leave.setDate(request.getDate());
        }

        if (request.getLeaveCategoryId() != null) {
            leave.setLeaveCategory(leaveCategoryService.getLeaveCategoryById(request.getLeaveCategoryId()));
        }
        Optional.ofNullable(request.getDuration()).ifPresent(leave::setDuration);
        Optional.ofNullable(request.getStartTime()).ifPresent(leave::setStartTime);
        Optional.ofNullable(request.getDescription()).ifPresent(leave::setDescription);

        Leave savedLeave = leaveRepository.save(leave);

        boolean categoryChanged = request.getLeaveCategoryId() != null;
        boolean durationChanged = request.getDuration() != null;

        if (categoryChanged || durationChanged) {
            annualLeaveService.syncOnLeaveUpdated(leave.getUser(), oldCategoryName, savedLeave.getLeaveCategory().getName(),
                    oldDuration, savedLeave.getDuration(), savedLeave.getDate().getYear());
        }

        return mapToUpdateLeaveResponse(savedLeave);
    }

    public void validateUpdateRequestNotEmpty(UpdateLeaveRequest request) {
        boolean hasField = Stream.of(
                request.getDate(),
                request.getStartTime(),
                request.getDescription(),
                request.getDuration(),
                request.getLeaveCategoryId()
        ).anyMatch(Objects::nonNull);

        if (!hasField) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "At least one field must be provided to update");
        }
    }

    private UpdateLeaveResponse mapToUpdateLeaveResponse(Leave leave) {
        return new UpdateLeaveResponse(
                leave.getId(),
                leave.getDate(),
                leave.getLeaveCategory().getName(),
                leave.getDuration(),
                leave.getStartTime(),
                leave.getDescription()
        );
    }

    private void validateLeaveOwnership(Leave leave, UUID userId, String errorMessage) {
        if (!isValidLeaveOwner(leave, userId)) {
            throw new HttpException(HttpStatus.FORBIDDEN,
                    errorMessage);
        }
    }

    private void validateExistingLeaveDate(LocalDate existingDate) {
        if (!isValidLeaveDate(existingDate)) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Cannot edit a leave that is no longer within the updatable date range");
        }
    }

    private void validateNewLeaveDate(LocalDate newDate) {
        if (!isValidLeaveDate(newDate)) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "New date must be within the current month for past dates, or within the current year for future dates");
        }
    }

    private void validateNewLeaveDateIsNotWeekend(LocalDate newDate) {
        if (isWeekendDay(newDate)) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Cannot update leave to a weekend date");
        }
    }

    private void validateNoDateConflict(UUID userId, UUID leaveId, LocalDate newDate) {
        boolean hasConflict = leaveRepository.existsByUserIdAndDateAndIdNotAndDeletedAtIsNull(userId, newDate, leaveId);
        if (hasConflict) {
            throw new HttpException(HttpStatus.CONFLICT,
                    "You already have a leave applied on this date");
        }
    }

    private void validateLeaveAlreadyCancelled(Leave leave) {
        if (leave.getDeletedAt() != null) {
            throw new HttpException(HttpStatus.CONFLICT, "Leave is already cancelled");
        }
    }

    private void validatePastLeaveDate(LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "Cannot cancel a past leave");
        }
    }

    public void deleteLeave(UUID leaveId, UUID userId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "Leave not found"));

        validateLeaveOwnership(leave, userId, "Not allowed to cancel this leave");
        validateLeaveAlreadyCancelled(leave);
        validatePastLeaveDate(leave.getDate());

        leave.setDeletedAt(LocalDateTime.now());
        leaveRepository.save(leave);

        String type = getLeaveType(leave);

        if (type.equals(LeaveConstants.ANNUAL_LEAVE)) {
            annualLeaveService.syncOnLeaveDeleted(leave.getUser(), leave.getDuration(), leave.getDate().getYear());
        }
    }
}
