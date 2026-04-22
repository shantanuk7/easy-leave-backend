package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.CreateRequestPayload;
import com.technogise.leave_management_system.dto.CreateRequestResponse;
import com.technogise.leave_management_system.enums.RequestStatus;
import com.technogise.leave_management_system.enums.RequestType;
import com.technogise.leave_management_system.enums.WeekendDay;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.RequestRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class RequestService {

    private final RequestRepository requestRepository;
    private final LeaveRepository leaveRepository;
    private final UserService userService;
    private final LeaveCategoryService leaveCategoryService;

    public RequestService(RequestRepository requestRepository,
                          LeaveRepository leaveRepository,
                          UserService userService,
                          LeaveCategoryService leaveCategoryService) {
        this.requestRepository = requestRepository;
        this.leaveRepository = leaveRepository;
        this.userService = userService;
        this.leaveCategoryService = leaveCategoryService;
    }

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Transactional
    public List<CreateRequestResponse> raiseRequest(CreateRequestPayload payload, UUID userId) {
        userService.getUserByUserId(userId);
        validatePastLeaveCategoryPresent(payload);

        if (payload.getRequestType() == RequestType.PAST_LEAVE) {
            leaveCategoryService.getLeaveCategoryById(payload.getLeaveCategoryId());
            List<LocalDate> validDates = filterValidPastLeaveDates(payload.getDates());
            List<LocalDate> workingDays = filterWeekendDates(validDates);
            validateNoDuplicateRequests(workingDays, userId);
        }

        return null;
    }

    private void validateNoDuplicateRequests(List<LocalDate> dates, UUID userId) {
        List<RequestStatus> activeStatuses = List.of(RequestStatus.PENDING, RequestStatus.APPROVED);
        dates.forEach(date -> {
            if (requestRepository.existsByRequestedByUserIdAndDateAndStatusIn(userId, date, activeStatuses)) {
                throw new HttpException(HttpStatus.CONFLICT,
                        "A request already exists for this date");
            }
        });
    }

    private List<LocalDate> filterValidPastLeaveDates(List<LocalDate> dates) {
        List<LocalDate> validDates = dates.stream()
                .filter(this::isValidPastMonthLeaveDate)
                .toList();

        if (validDates.isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Past leave dates must be from a previous month within the current year");
        }

        return validDates;
    }

    private List<LocalDate> filterWeekendDates(List<LocalDate> dates) {
        List<LocalDate> workingDays = dates.stream()
                .filter(date -> !isWeekendDay(date))
                .toList();

        if (workingDays.isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Cannot apply for leave on weekends.");
        }

        return workingDays;
    }

    private boolean isValidPastMonthLeaveDate(LocalDate date) {
        LocalDate today = LocalDate.now(IST);
        LocalDate firstDayOfPreviousMonth = today.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayOfPreviousMonth = today.withDayOfMonth(1).minusDays(1);

        return !date.isBefore(firstDayOfPreviousMonth) && !date.isAfter(lastDayOfPreviousMonth);
    }

    private boolean isWeekendDay(LocalDate date) {
        return Arrays.stream(WeekendDay.values())
                .anyMatch(weekend -> weekend.getDayOfWeek() == date.getDayOfWeek());
    }

    private void validatePastLeaveCategoryPresent(CreateRequestPayload payload) {
        if (payload.getRequestType() == RequestType.PAST_LEAVE
                && payload.getLeaveCategoryId() == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Leave category is required for Past Leave requests");
        }
    }
}
