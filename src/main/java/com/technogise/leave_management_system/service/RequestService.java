package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.RequestResponse;
import com.technogise.leave_management_system.dto.CreateRequestResponse;
import com.technogise.leave_management_system.dto.CreateRequestPayload;
import com.technogise.leave_management_system.dto.UpdateLeaveRequest;
import com.technogise.leave_management_system.dto.UpdateRequestPayload;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.Request;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.entity.AnnualLeave;
import com.technogise.leave_management_system.enums.RequestStatus;
import com.technogise.leave_management_system.enums.RequestType;
import com.technogise.leave_management_system.enums.ScopeType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.enums.WeekendDay;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.AnnualLeaveRepository;
import com.technogise.leave_management_system.repository.LeaveRepository;
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
    private final LeaveRepository leaveRepository;
    private final LeaveService leaveService;
    private final AnnualLeaveRepository annualLeaveRepository;

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");


    public RequestService(RequestRepository requestRepository,
                          UserService userService,
                          LeaveCategoryService leaveCategoryService,
                          LeaveRepository leaveRepository,
                          LeaveService leaveService,
                          AnnualLeaveRepository annualLeaveRepository
    ) {
        this.requestRepository = requestRepository;
        this.userService = userService;
        this.leaveCategoryService = leaveCategoryService;
        this.leaveRepository = leaveRepository;
        this.leaveService = leaveService;
        this.annualLeaveRepository = annualLeaveRepository;
    }

    private Page<Request> getRequestsForSelf(User user, RequestStatus status, Pageable pageable) {
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

    private Page<Request> getRequestsForOrganization(User user, RequestStatus status, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "date")
        );
        if (user.getRole() != UserRole.MANAGER) {
            throw new HttpException(HttpStatus.FORBIDDEN, "Not Allowed to access this resource");
        }
        if (status != null && status != RequestStatus.PENDING) {
            throw new HttpException(HttpStatus.BAD_REQUEST, "Managers can only access pending requests");
        }
        return requestRepository.findAllByStatus(RequestStatus.PENDING, sortedPageable);
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
                request.getCreatedAt().toLocalDate(),
                request.getManagerRemark()
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
        } else if (scope == ScopeType.ORGANIZATION) {
            requests = getRequestsForOrganization(user,status, pageable);
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

    private UpdateLeaveRequest mapToUpdateLeaveRequest(Request request) {
        return new UpdateLeaveRequest(
                request.getDate(),
                request.getStartTime(),
                request.getDescription(),
                request.getDuration(),
                request.getLeaveCategory().getId(),
                "request"
        );
    }

    public RequestResponse handleRequest(User manager, UUID requestId, UpdateRequestPayload payload) {
        Request request = requestRepository.findById(requestId).orElseThrow(
                () -> new HttpException(HttpStatus.NOT_FOUND, "Request not found with Id: " + requestId));

        validateRequestStatus(request);

        if (payload.getStatus() == RequestStatus.REJECTED) {
            return finalizeRequest(request, manager, payload);
        }

        if (request.getRequestType() == RequestType.PAST_LEAVE) {
            return handlePastLeaveRequest(request, manager, payload);
        } else if (request.getRequestType() == RequestType.COMPENSATORY_OFF) {
            return handleCompOffRequest(request, manager, payload);
        }

        throw new HttpException(HttpStatus.BAD_REQUEST, "Unsupported request type");
    }

    private void validateRequestStatus(Request request) {
        if (request.getStatus() == RequestStatus.APPROVED
                || request.getStatus() == RequestStatus.REJECTED) {

            throw new HttpException(
                    HttpStatus.BAD_REQUEST,
                    "This request has already been processed and cannot be modified"
            );
        }
    }

    private RequestResponse handlePastLeaveRequest(Request request, User manager, UpdateRequestPayload payload) {
        Leave existingLeave = leaveRepository
                .findByUserIdAndDate(request.getRequestedByUser().getId(), request.getDate())
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "Leave not found"));

        UpdateLeaveRequest updateRequest = mapToUpdateLeaveRequest(request);
        leaveService.updateLeave(existingLeave.getId(), updateRequest, existingLeave.getUser().getId());

        return finalizeRequest(request, manager, payload);
    }

    private RequestResponse handleCompOffRequest(Request request, User manager, UpdateRequestPayload payload) {
        int currentYear = LocalDate.now().getYear();
        UUID userId = request.getRequestedByUser().getId();

        AnnualLeave annualLeave = annualLeaveRepository.findByUserIdAndYear(
                userId,
                String.valueOf(currentYear)
        ).orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "Annual record not found for user: " + userId));

        double compOffDuration = (request.getDuration() == DurationType.FULL_DAY) ? 1.0 : 0.5;
        double updatedCount = annualLeave.getCompensatoryOffCount() + compOffDuration;
        annualLeave.setCompensatoryOffCount(updatedCount);
        annualLeaveRepository.save(annualLeave);

        return finalizeRequest(request, manager, payload);
    }

    private RequestResponse finalizeRequest(Request request, User manager, UpdateRequestPayload payload) {
        request.setStatus(payload.getStatus());
        request.setActionedByManager(manager);

        if (payload.getManagerRemark() != null && !payload.getManagerRemark().isBlank()) {
            request.setManagerRemark(payload.getManagerRemark());
        }

        Request savedRequest = requestRepository.save(request);
        return mapToRequestResponse(savedRequest);
    }

    public String getResponseMessage(RequestResponse response) {
        if (response.getStatus() == RequestStatus.REJECTED) {
            return "Request rejected successfully";
        }
        return "Request approved successfully";
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
                .filter(this::isValidRequestDateRange)
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
                .filter(this::isValidRequestDateRange)
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
                    "A request already exists for one or more of the selected dates. Please choose different dates.");
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

    private boolean isValidRequestDateRange(LocalDate date) {
        LocalDate today = LocalDate.now(IST);
        LocalDate thirtyDaysAgo = today.minusDays(30);
        return !date.isBefore(thirtyDaysAgo) && date.isBefore(today);
    }

    private boolean isWeekendDay(LocalDate date) {
        return Arrays.stream(WeekendDay.values())
                .anyMatch(weekend -> weekend.getDayOfWeek() == date.getDayOfWeek());
    }
}

