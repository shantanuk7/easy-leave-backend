package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.RequestResponse;
import com.technogise.leave_management_system.entity.Request;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.RequestStatus;
import com.technogise.leave_management_system.enums.ScopeType;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.RequestRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.UUID;
@Service
public class RequestService {

    private final RequestRepository requestRepository;
    private final UserService userService;

    public RequestService(RequestRepository requestRepository, UserService userService) {
        this.requestRepository = requestRepository;
        this.userService = userService;
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
}

