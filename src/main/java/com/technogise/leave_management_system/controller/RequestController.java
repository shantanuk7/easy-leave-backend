package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.RequestResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.RequestStatus;
import com.technogise.leave_management_system.enums.ScopeType;
import com.technogise.leave_management_system.response.SuccessResponse;
import com.technogise.leave_management_system.service.RequestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.technogise.leave_management_system.enums.ScopeType.DEFAULT_SCOPE;

@RestController
@RequestMapping("/api/requests")
public class RequestController {
    private final RequestService requestService;
    public  RequestController(RequestService requestService) {
        this.requestService = requestService;
    }
    @GetMapping
    public ResponseEntity<SuccessResponse<Page<RequestResponse>>> getAllRequests(
            Pageable pageable,
            @RequestParam(defaultValue = DEFAULT_SCOPE) ScopeType scope,
            @RequestParam(required = false) RequestStatus status,
            @AuthenticationPrincipal User user
    ) {
        Page<RequestResponse> response =
                requestService.getAllRequests(pageable, user.getId(), scope, status);
        return ResponseEntity.status(HttpStatus.OK).body(
                SuccessResponse.success("Requests fetch Successfully", response));
    }
}
