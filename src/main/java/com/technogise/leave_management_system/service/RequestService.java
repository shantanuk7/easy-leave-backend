package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.CreateRequestPayload;
import com.technogise.leave_management_system.dto.CreateRequestResponse;
import com.technogise.leave_management_system.dto.RequestResponse;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.Request;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.RequestStatus;
import com.technogise.leave_management_system.enums.RequestType;
import com.technogise.leave_management_system.enums.ScopeType;
import com.technogise.leave_management_system.enums.WeekendDay;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.RequestRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final UserService userService;
    private final LeaveCategoryService leaveCategoryService;

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");


    public RequestService(RequestRepository requestRepository,
                          UserService userService,
                          LeaveCategoryService leaveCategoryService) {
        this.requestRepository = requestRepository;
        this.userService = userService;
        this.leaveCategoryService = leaveCategoryService;
    }

    private Page<Request> getRequestsForSelf( User user, RequestStatus status, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "date")
        );
        if (status != null) {
            return requestRepository.findAllByRequestedByUserIdAndStatus(user.getId(), status, sortedPageable);
        } else {
            return requestRepository.findAllByRequestedByUserId(user.getId(), sortedPageable);
        }
    }

    private RequestResponse mapToRequestResponse(Request request) {
        return new RequestResponse(
                request.getId(),
                request.getRequestedByUser().getName(),
                request.getRequestType(),
                request.getLeaveCategory() != null
                        ? request.getLeaveCategory().getName()
                        : null,
                request.getDate(),
                request.getDuration(),
                request.getDescription(),
                request.getStatus(),
                request.getCreatedAt().toLocalDate()
        );
    }

    public Page<RequestResponse> getAllRequests(
            Pageable pageable,
            UUID userId,
            ScopeType scope,
            RequestStatus status
    ) {
        User user = userService.getUserByUserId(userId);
        Page<Request> requests;
        if (scope == ScopeType.SELF) {
            requests = getRequestsForSelf(user, status, pageable);
        } else {
            throw new HttpException(HttpStatus.BAD_REQUEST, "Invalid scope");
        }
        return requests.map(this::mapToRequestResponse);
    }
    @Transactional
    public List<CreateRequestResponse> raiseRequest(CreateRequestPayload payload, UUID userId) {
        User user = userService.getUserByUserId(userId);

        if (payload.getRequestType() == RequestType.COMPENSATORY_OFF) {
            return raiseCompOffRequest(payload, user);
        }

        validatePastLeaveCategoryPresent(payload);
        LeaveCategory leaveCategory = leaveCategoryService
                .getLeaveCategoryById(payload.getLeaveCategoryId());
        List<LocalDate> validDates = filterValidPastLeaveDates(payload.getDates());
        List<LocalDate> workingDays = filterWeekendDates(validDates);
        validateNoDuplicateRequests(workingDays, userId);
        return savePastLeaveRequests(workingDays, payload, user, leaveCategory);
    }

    private List<CreateRequestResponse> raiseCompOffRequest(CreateRequestPayload payload, User user) {
        List<LocalDate> validRangeDates = filterValidCompOffDates(payload.getDates());
        List<LocalDate> weekendDates = filterNonWeekendDates(validRangeDates);
        validateNoDuplicateRequests(weekendDates, user.getId());
        return saveCompOffRequests(weekendDates, payload, user);

    }

    private List<LocalDate> filterNonWeekendDates(List<LocalDate> dates) {
        List<LocalDate> weekendDates = dates.stream()
                .filter(this::isWeekendDay)
                .toList();

        if (weekendDates.isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Compensatory off dates must fall on a weekend (Saturday or Sunday)");
        }
        return weekendDates;
    }

    private List<LocalDate> filterValidCompOffDates(List<LocalDate> dates) {
        List<LocalDate> validDates = dates.stream()
                .filter(this::isValidPastLeaveDate)
                .toList();

        if (validDates.isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Compensatory off dates must be within the last 30 days");
        }
        return validDates;
    }


    private void validatePastLeaveCategoryPresent(CreateRequestPayload payload) {
        if (payload.getLeaveCategoryId() == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Leave category is required for Past Leave requests");
        }
    }

    private List<LocalDate> filterValidPastLeaveDates(List<LocalDate> dates) {
        List<LocalDate> validDates = dates.stream()
                .filter(this::isValidPastLeaveDate)
                .toList();

        if (validDates.isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Past leave dates must be within the last 30 days");
        }

        return validDates;
    }

    private List<LocalDate> filterWeekendDates(List<LocalDate> dates) {
        List<LocalDate> workingDays = dates.stream()
                .filter(date -> !isWeekendDay(date))
                .toList();

        if (workingDays.isEmpty()) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Cannot raise the request for weekend leave.");
        }

        return workingDays;
    }

    private void validateNoDuplicateRequests(List<LocalDate> dates, UUID userId) {
        List<RequestStatus> activeStatuses = List.of(RequestStatus.PENDING, RequestStatus.APPROVED);

        if (requestRepository.existsByRequestedByUserIdAndDateInAndStatusIn(
                userId, dates, activeStatuses)) {
            throw new HttpException(HttpStatus.CONFLICT,
                    "A request already exists for one of the selected dates");
        }
    }

    private List<CreateRequestResponse> saveCompOffRequests(
            List<LocalDate> dates, CreateRequestPayload payload, User user) {

        List<Request> requests = dates.stream().map(date -> {
            Request request = new Request();
            request.setRequestedByUser(user);
            request.setRequestType(payload.getRequestType());
            request.setLeaveCategory(null);
            request.setDate(date);
            request.setStartTime(payload.getStartTime());
            request.setDuration(payload.getDuration());
            request.setDescription(payload.getDescription());
            request.setStatus(RequestStatus.PENDING);
            return request;
        }).toList();

        List<Request> saved = requestRepository.saveAll(requests);

        return saved.stream()
                .map(r -> new CreateRequestResponse(
                        r.getId(),
                        r.getRequestType(),
                        null,
                        r.getDate(),
                        r.getStartTime(),
                        r.getDuration(),
                        r.getDescription(),
                        r.getStatus()
                ))
                .toList();
    }

    private List<CreateRequestResponse> savePastLeaveRequests(
            List<LocalDate> dates,
            CreateRequestPayload payload,
            User user,
            LeaveCategory leaveCategory) {
        List<Request> requests = dates.stream().map(date -> {
            Request request = new Request();
            request.setRequestedByUser(user);
            request.setRequestType(payload.getRequestType());
            request.setLeaveCategory(leaveCategory);
            request.setDate(date);
            request.setStartTime(payload.getStartTime());
            request.setDuration(payload.getDuration());
            request.setDescription(payload.getDescription());
            request.setStatus(RequestStatus.PENDING);
            return request;
        }).toList();

        List<Request> savedRequests = requestRepository.saveAll(requests);

        return savedRequests.stream()
                .map(request -> new CreateRequestResponse(
                        request.getId(),
                        request.getRequestType(),
                        request.getLeaveCategory().getName(),
                        request.getDate(),
                        request.getStartTime(),
                        request.getDuration(),
                        request.getDescription(),
                        request.getStatus()
                ))
                .toList();
    }

    private boolean isValidPastLeaveDate(LocalDate date) {
        LocalDate today = LocalDate.now(IST);
        LocalDate thirtyDaysAgo = today.minusDays(30);
        return !date.isBefore(thirtyDaysAgo) && date.isBefore(today);
    }

    private boolean isWeekendDay(LocalDate date) {
        return Arrays.stream(WeekendDay.values())
                .anyMatch(weekend -> weekend.getDayOfWeek() == date.getDayOfWeek());
    }
}

