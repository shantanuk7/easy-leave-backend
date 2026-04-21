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

        return null;
    }

    private void validatePastLeaveCategoryPresent(CreateRequestPayload payload) {
        if (payload.getRequestType() == RequestType.PAST_LEAVE
                && payload.getLeaveCategoryId() == null) {
            throw new HttpException(HttpStatus.BAD_REQUEST,
                    "Leave category is required for Past Leave requests");
        }
    }
}
