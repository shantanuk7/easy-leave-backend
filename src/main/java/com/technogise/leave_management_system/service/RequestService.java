package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.CreateRequestPayload;
import com.technogise.leave_management_system.dto.CreateRequestResponse;
import com.technogise.leave_management_system.enums.RequestType;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.RequestRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
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

    @Transactional
    public List<CreateRequestResponse> raiseRequest(CreateRequestPayload payload, UUID userId) {
        userService.getUserByUserId(userId);
        validatePastLeaveCategoryPresent(payload);

        if (payload.getRequestType() == RequestType.PAST_LEAVE) {
            leaveCategoryService.getLeaveCategoryById(payload.getLeaveCategoryId());
            validatePastLeaveDates(payload.getDates());
        }

        return null;
    }

    private void validatePastLeaveDates(List<LocalDate> dates) {
        boolean allInvalid = dates.stream().allMatch(date -> !isValidPastMonthLeaveDate(date));
        if (allInvalid) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Past leave dates must be from a previous month within the current year");
        }
    }

    private boolean isValidPastMonthLeaveDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        LocalDate firstDayOfPreviousMonth = today.minusMonths(1).withDayOfMonth(1);
        LocalDate lastDayOfPreviousMonth = today.withDayOfMonth(1).minusDays(1);

        return !date.isBefore(firstDayOfPreviousMonth) && !date.isAfter(lastDayOfPreviousMonth);
    }

    private void validatePastLeaveCategoryPresent(CreateRequestPayload payload) {
        if (payload.getRequestType() == RequestType.PAST_LEAVE
                && payload.getLeaveCategoryId() == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Leave category is required for Past Leave requests");
        }
    }
}
