package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.CreateLeaveRequest;
import com.technogise.leave_management_system.dto.CreateLeaveResponse;
import com.technogise.leave_management_system.dto.LeaveResponse;
import com.technogise.leave_management_system.dto.UpdateLeaveResponse;
import com.technogise.leave_management_system.dto.UpdateLeaveRequest;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.ScopeType;
import com.technogise.leave_management_system.response.SuccessResponse;
import com.technogise.leave_management_system.service.LeaveService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @GetMapping
    public ResponseEntity<SuccessResponse<List<LeaveResponse>>> findAllLeave(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "scope", defaultValue = ScopeType.DEFAULT_SCOPE) String scope,
            @RequestParam(name = "empId", required = false) UUID empId,
            @RequestParam(name = "year", required = false) Integer year,
            @AuthenticationPrincipal User user
    ) {
        log.info("GET /api/leaves called by userId={}, scope={}, status={}, empId={}, year={}",
                user.getId(), scope, status, empId, year);

        List<LeaveResponse> leaveList = leaveService.getAllLeaves(user.getId(), scope, status, empId, year);

        log.debug("Returning {} leaves for userId={}", leaveList.size(), user.getId());

        String message = leaveList.isEmpty() ? "No leave found" : "Leaves retrieved successfully";

        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.success(message, leaveList));
    }
    @PostMapping
    public ResponseEntity<SuccessResponse<List<CreateLeaveResponse>>> applyLeave(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateLeaveRequest createLeaveRequest
    ) {
        log.info("POST /api/leaves called by userId={}, dates={}, duration={}",
                user.getId(), createLeaveRequest.getDates(), createLeaveRequest.getDuration());
        UUID userId = user.getId();
        List<CreateLeaveResponse> createLeaveResponses = leaveService.applyLeave(createLeaveRequest, userId);
        log.debug("Leave application successful for userId={}, {} leave(s) created", user.getId(), createLeaveResponses.size());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.success("Leaves applied successfully", createLeaveResponses));
    }

    @GetMapping("/{leaveId}")
    public ResponseEntity<SuccessResponse<LeaveResponse>> getLeaveById(
            @AuthenticationPrincipal User user,
            @PathVariable UUID leaveId
    ) {
        log.info("GET /api/leaves/{} called by userId={}", leaveId, user.getId());
        UUID userId = user.getId();
        LeaveResponse leaveResponse = leaveService.getLeaveById(leaveId, userId);
        log.info("Returning {} leave for userId={}", leaveResponse, userId.toString());
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.success("Leave retrieved successfully", leaveResponse));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SuccessResponse<UpdateLeaveResponse>> updateLeave(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLeaveRequest updateLeaveRequest,
            @AuthenticationPrincipal User user
    ) {
        log.info("PATCH /api/leaves/{} called by userId={}", id, user.getId());

        UpdateLeaveResponse updateLeaveResponse = leaveService.updateLeave(
                id, updateLeaveRequest, user.getId());

        log.debug("Leave updated successfully leaveId={}, userId={}", id, user.getId());

        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.success("Leave updated successfully", updateLeaveResponse));
    }
}
