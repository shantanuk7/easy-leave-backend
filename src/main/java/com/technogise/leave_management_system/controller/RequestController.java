package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.CreateRequestPayload;
import com.technogise.leave_management_system.dto.CreateRequestResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.response.SuccessResponse;
import com.technogise.leave_management_system.service.RequestService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<List<CreateRequestResponse>>> raiseRequest(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateRequestPayload payload
    ) {
        log.info("POST /api/requests called by userId={}, requestType={}, dates={}",
                user.getId(), payload.getRequestType(), payload.getDates());

        List<CreateRequestResponse> responses = requestService.raiseRequest(payload, user.getId());

        log.debug("Request raised successfully for userId={}, {} request(s) created",
                user.getId(), responses.size());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.success("Request(s) raised successfully", responses));
    }
}
