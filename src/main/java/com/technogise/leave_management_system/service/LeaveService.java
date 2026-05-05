package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.constants.LeaveConstants;
import com.technogise.leave_management_system.dto.CreateLeaveRequest;
import com.technogise.leave_management_system.dto.CreateLeaveResponse;
import com.technogise.leave_management_system.dto.UpdateLeaveRequest;
import com.technogise.leave_management_system.dto.UpdateLeaveResponse;
import com.technogise.leave_management_system.entity.Holiday;
import com.technogise.leave_management_system.dto.LeaveResponse;
import com.technogise.leave_management_system.dto.LeaveFilterRequest;
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
import com.technogise.leave_management_system.specification.LeaveSpecification;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    public static final String REQUEST_TYPE = "request";

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

    double computeTakenDays(UUID userId, UUID categoryId, int year, UUID excludeLeaveId) {
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        return leaveRepository
                .findAllByUserIdAndDateBetweenAndDeletedAtIsNull(userId, startDate, endDate, Sort.unsorted())
                .stream()
                .filter(leave -> leave.getLeaveCategory() != null && leave.getLeaveCategory().getId().equals(categoryId))
                .filter(leave -> excludeLeaveId == null || !leave.getId().equals(excludeLeaveId))
                .mapToDouble(leave -> leave.getDuration() == DurationType.FULL_DAY ? 1.0 : 0.5)
                .sum();
    }
    void validateNonAnnualBalanceSufficiency(
            LeaveCategory category,
            double requested,
            UUID userId,
            int year,
            UUID excludeLeaveId) {
        if (category == null || category.getName().equals(LeaveConstants.ANNUAL_LEAVE)) {
            return;
        }
        double taken     = computeTakenDays(userId, category.getId(), year, excludeLeaveId);
        double remaining = category.getAllocatedDays() - taken;
        if (requested > remaining) {
            throw new HttpException(
                    HttpStatus.BAD_REQUEST,
                    "Insufficient leave balance for " + category.getName()
            );
        }
    }

    private void validateDurationForCategory(LeaveCategory category, DurationType duration) {
        if (category == null) {
            return;
        }
        if (!category.getName().equals(LeaveConstants.ANNUAL_LEAVE) && duration == DurationType.HALF_DAY) {
            throw new HttpException(HttpStatus.BAD_REQUEST, category.getName() + " can only be applied as a full day");
        }
    }

    private LeaveResponse mapToLeaveResponse(Leave leave) {
        String type = getLeaveDisplayName(leave);
        return new LeaveResponse(
                leave.getId(),
                leave.getDate(),
                leave.getUser().getName(),
                type,
                leave.getDuration(),
                leave.getStartTime(),
                leave.getUpdatedAt(),
                leave.getDescription(),
                leave.getHoliday() != null ? leave.getHoliday().getId() : null
        );
    }

    public Page<LeaveResponse> getAllLeaves(
            UUID userId,
            LeaveFilterRequest filter,
            Pageable pageable
    ) {
        User user = userService.getUserByUserId(userId);
        String scope = filter.getScope();

        if (filter.getEmpId() != null && !scope.equalsIgnoreCase(ORGANIZATION.toString())) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "empId is only allowed when scope is ORGANIZATION");
        }

        Specification<Leave> spec = buildSpecification(user, filter);

        Page<Leave> leaves = leaveRepository.findAll(spec, pageable);
        return leaves.map(this::mapToLeaveResponse);
    }

    private Specification<Leave> buildSpecification(User user, LeaveFilterRequest filter) {
        Specification<Leave> spec = Specification
                .where(LeaveSpecification.notDeleted());

        spec = spec.and(applyScopeFilter(user, filter));

        if (filter.getStatus() != null && !filter.getStatus().isBlank()) {
            Specification<Leave> statusSpec = LeaveSpecification.leavesByStatus(filter.getStatus());
            spec = spec.and(statusSpec);
        }

        if (filter.getYear() != null) {
            spec = spec.and(LeaveSpecification.leavesWithinYear(filter.getYear()));
        }

        return spec;
    }

    private Specification<Leave> applyScopeFilter(User user, LeaveFilterRequest filter) {
        String scope = filter.getScope();

        if (scope.equalsIgnoreCase(SELF.toString())) {
            return LeaveSpecification.allLeavesOfEmployee(user.getId());
        } else if (scope.equalsIgnoreCase(ORGANIZATION.toString())) {
            if (!user.getRole().equals(UserRole.MANAGER)) {
                throw new HttpException(HttpStatus.FORBIDDEN, "Not Allowed to access this resource");
            }

            if (filter.getEmpId() != null) {
                userService.getUserByUserId(filter.getEmpId());
                return LeaveSpecification.allLeavesOfEmployee(filter.getEmpId());
            }

            return LeaveSpecification.noFilter();
        }

        throw new HttpException(HttpStatus.BAD_REQUEST, "Invalid scope query parameter");
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

    private String getLeaveDisplayName(Leave leave) {
        if (leave.getLeaveCategory() != null) {
            return leave.getLeaveCategory().getName();
        }
        return leave.getHoliday().getType().getDisplayName();
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

        validateDurationForCategory(category, request.getDuration());

        List<LocalDate> workingDates = filterValidWorkingDates(request.getDates());
        List<LocalDate> newDates = filterNonOverlappingLeaveDates(userId, workingDates);

        if (hasCategory) {
            double requestedDays = newDates.size() * (request.getDuration() == DurationType.FULL_DAY ? 1.0 : 0.5);
            validateNonAnnualBalanceSufficiency(category, requestedDays, userId, LocalDate.now().getYear(), null);
        }

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

        if (category != null && category.getName().equalsIgnoreCase(LeaveConstants.ANNUAL_LEAVE)) {
            annualLeaveService.syncOnLeaveCreated(user, request.getDuration(), newDates.size(), LocalDate.now().getYear()); }

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

        String type = getLeaveDisplayName(leave);

        return new LeaveResponse(leave.getId(), leave.getDate(), leave.getUser().getName(), type,
                leave.getDuration(),
                leave.getStartTime(),
                leave.getUpdatedAt(),
                leave.getDescription(),
                leave.getHoliday() != null ? leave.getHoliday().getId() : null
        );
    }

    private List<LocalDate> filterValidWorkingDates(List<LocalDate> requestedDates) {

        List<LocalDate> validDates = requestedDates.stream()
                .filter(this::isValidLeaveDate)
                .toList();

        if (validDates.isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Some selected dates are outside the allowed range.");
        }

        List<LocalDate> workingDays = validDates.stream()
                .filter(date -> !isWeekendDay(date))
                .filter(date -> !isFixedHoliday(date))
                .toList();

        if (workingDays.isEmpty()) {
            throw new HttpException(
                    HttpStatus.BAD_REQUEST,
                    "The selected date(s) fall on a weekend or fixed holiday. Please choose a working day."
            );
        }

        return workingDays;
    }

    private List<LocalDate> filterNonOverlappingLeaveDates(UUID userId, List<LocalDate> dates) {

        Set<LocalDate> existingDates = leaveRepository
                .findAllByUserIdAndDeletedAtNull(userId)
                .stream()
                .map(Leave::getDate)
                .collect(Collectors.toSet());

        List<LocalDate> newDates = dates.stream()
                .filter(date -> !existingDates.contains(date))
                .toList();

        if (newDates.isEmpty()) {
            throw new HttpException(
                    HttpStatus.CONFLICT,
                    "Leave has already been applied for all the selected dates. Please choose different dates."
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

        boolean requestHasHoliday  = request.getHolidayId() != null;
        boolean requestHasCategory = request.getLeaveCategoryId() != null;

        if (requestHasHoliday && requestHasCategory) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Cannot apply for a leave with both fields provided. Provide either holidayId or leaveCategoryId.");
        }

        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "Leave not found with id: " + leaveId));

        validateLeaveOwnership(leave, userId, "Not allowed to update this leave");
        if (REQUEST_TYPE.equalsIgnoreCase(request.getType())) {
            validateNewRequestLeaveDate(request.getDate());
        } else {
            validateExistingLeaveDate(leave.getDate());
        }

        LeaveCategory targetCategory = requestHasHoliday
                ? null
                : (requestHasCategory
                ? leaveCategoryService.getLeaveCategoryById(request.getLeaveCategoryId())
                : leave.getLeaveCategory());

        Holiday targetHoliday = requestHasHoliday
                ? holidayService.getHolidayById(request.getHolidayId())
                : (requestHasCategory ? null : leave.getHoliday());

        DurationType targetDuration = request.getDuration() != null
                ? request.getDuration()
                : leave.getDuration();

        double requestedDays = targetDuration == DurationType.FULL_DAY ? 1.0 : 0.5;

        validateNonAnnualBalanceSufficiency(targetCategory, requestedDays, userId, leave.getDate().getYear(), leaveId);
        validateDurationForCategoryWithHoliday(targetCategory, targetHoliday, targetDuration);

        if (request.getDate() != null) {
            if (REQUEST_TYPE.equalsIgnoreCase(request.getType())) {
                validateNewRequestLeaveDate(request.getDate());
            } else {
                validateNewLeaveDate(request.getDate());
            }
            validateNewLeaveDateIsNotWeekend(request.getDate());
            validateNoDateConflict(userId, leaveId, request.getDate());
            validateNewLeaveDateIsNotHoliday(request.getDate());
            leave.setDate(request.getDate());
        }

        String oldCategoryName = leave.getLeaveCategory() != null ? leave.getLeaveCategory().getName() : null;
        DurationType oldDuration = leave.getDuration();

        if (requestHasHoliday) {
            User user = userService.getUserByUserId(userId);
            Holiday holiday = holidayService.getHolidayById(request.getHolidayId());
            if (leave.getHoliday() == null) {
                validateOptionalHolidaysCount(user);
            }
            leave.setHoliday(holiday);
            leave.setLeaveCategory(null);
        } else if (requestHasCategory) {
            leave.setLeaveCategory(targetCategory);
            leave.setHoliday(null);
        }

        leave.setDuration(targetDuration);
        Optional.ofNullable(request.getStartTime()).ifPresent(leave::setStartTime);
        Optional.ofNullable(request.getDescription()).ifPresent(leave::setDescription);

        Leave savedLeave = leaveRepository.save(leave);

        String newCategoryName = savedLeave.getLeaveCategory() != null ? savedLeave.getLeaveCategory().getName() : null;

        if (isAnnualLeaveSyncRequired(oldCategoryName, newCategoryName, requestHasCategory || requestHasHoliday, request.getDuration() != null)) {
            annualLeaveService.syncOnLeaveUpdated(
                    savedLeave.getUser(),
                    oldCategoryName,
                    newCategoryName,
                    oldDuration,
                    savedLeave.getDuration(),
                    savedLeave.getDate().getYear()
            );
        }

        leaveIntegrationHandler.handleLeaveUpdate(savedLeave);
        return mapToUpdateLeaveResponse(savedLeave);
    }

    public void validateNewRequestLeaveDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        LocalDate thirtyDaysAgo = today.minusDays(30);

        if (date.getYear() != today.getYear()) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "Date must be within the current year");
        }

        if (date.isBefore(thirtyDaysAgo) || date.isAfter(today) || date.equals(today)) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "Date must be within the last 30 days excluding today");
        }
    }

    private void validateDurationForCategoryWithHoliday(LeaveCategory targetCategory, Holiday targetHoliday, DurationType duration) {
        if (duration != DurationType.HALF_DAY) {
            return;
        }
        boolean isAnnualLeave = targetCategory != null
                && targetCategory.getName().equals(LeaveConstants.ANNUAL_LEAVE);
        if (!isAnnualLeave) {
            String name = targetCategory != null ? targetCategory.getName() : targetHoliday.getType().getDisplayName();
            throw new HttpException(HttpStatus.BAD_REQUEST, name + " can only be applied as a full day");
        }
    }

    private boolean isAnnualLeaveSyncRequired(
            String oldCategoryName,
            String newCategoryName,
            boolean typeChangeRequested,
            boolean durationChangeRequested) {

        boolean oldWasAnnual = LeaveConstants.ANNUAL_LEAVE.equalsIgnoreCase(oldCategoryName);
        boolean newIsAnnual  = LeaveConstants.ANNUAL_LEAVE.equalsIgnoreCase(newCategoryName);

        return (typeChangeRequested && (oldWasAnnual || newIsAnnual))
                || (durationChangeRequested && newIsAnnual);
    }

    public void validateUpdateRequestNotEmpty(UpdateLeaveRequest request) {
        boolean hasField = Stream.of(
                request.getDate(),
                request.getStartTime(),
                request.getDescription(),
                request.getDuration(),
                request.getLeaveCategoryId(),
                request.getHolidayId()
        ).anyMatch(Objects::nonNull);

        if (!hasField) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Update at least one detail to save changes");
        }
    }

    private UpdateLeaveResponse mapToUpdateLeaveResponse(Leave leave) {
        String categoryName = (leave.getLeaveCategory() != null)
                ? leave.getLeaveCategory().getName()
                : leave.getHoliday().getType().getDisplayName();

        return new UpdateLeaveResponse(
                leave.getId(),
                leave.getDate(),
                categoryName,
                leave.getDuration(),
                leave.getStartTime(),
                leave.getDescription()
        );
    }

    private void validateLeaveOwnership(Leave leave, UUID userId, String errorMessage) {
        if (!isValidLeaveOwner(leave, userId)) {
            throw new HttpException(HttpStatus.FORBIDDEN, errorMessage);
        }
    }

    private void validateExistingLeaveDate(LocalDate existingDate) {
        if (!isValidLeaveDate(existingDate)) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Cannot edit a leave that is no longer within the updatable date range");
        }
    }

    private void validateNewLeaveDate(LocalDate newDate) {
        LocalDate today = LocalDate.now();

        if (newDate.getYear() != today.getYear()) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "The selected date must be within the current year.");
        }

        if (newDate.isBefore(today) && !newDate.getMonth().equals(today.getMonth())) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "Past dates can only be selected within the current month.");
        }
    }

    private void validateNewLeaveDateIsNotWeekend(LocalDate newDate) {
        if (isWeekendDay(newDate)) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Leave cannot be scheduled on a weekend. Please select a working day.");
        }
    }

    private void validateNoDateConflict(UUID userId, UUID leaveId, LocalDate newDate) {
        boolean hasConflict = leaveRepository
                .existsByUserIdAndDateAndIdNotAndDeletedAtIsNull(userId, newDate, leaveId);
        if (hasConflict) {
            throw new HttpException(HttpStatus.CONFLICT,
                    "Leave already exists for this date");
        }
    }

    private void validateNewLeaveDateIsNotHoliday(LocalDate newDate) {
        if (isFixedHoliday(newDate)) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Cannot update leave to a fixed holiday date");
        }
    }

    private void validateLeaveAlreadyCancelled(Leave leave) {
        if (leave.getDeletedAt() != null) {
            throw new HttpException(HttpStatus.CONFLICT, "Leave is already cancelled");
        }
    }

    private void validatePastLeaveDate(LocalDate date) {
        if (date.isBefore(LocalDate.now())) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "Leave on a past date cannot be cancelled.");
        }
    }
    @Transactional
    public void deleteLeave(UUID leaveId, UUID userId) {
        Leave leave = leaveRepository.findById(leaveId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "Leave not found"));

        validateLeaveOwnership(leave, userId, "Not allowed to cancel this leave");
        validateLeaveAlreadyCancelled(leave);
        validatePastLeaveDate(leave.getDate());

        leave.setDeletedAt(LocalDateTime.now());
        leaveRepository.save(leave);

        if (leave.getLeaveCategory() != null && leave.getLeaveCategory().getName().equals(LeaveConstants.ANNUAL_LEAVE)) {
            annualLeaveService.syncOnLeaveDeleted(leave.getUser(), leave.getDuration(), leave.getDate().getYear());
        }

        leaveIntegrationHandler.handleLeaveDelete(leave);
    }
}
