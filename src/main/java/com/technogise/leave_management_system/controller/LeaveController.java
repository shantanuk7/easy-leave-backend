package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.CreateLeaveRequest;
import com.technogise.leave_management_system.dto.CreateLeaveResponse;
import com.technogise.leave_management_system.dto.LeaveResponse;
import com.technogise.leave_management_system.dto.UpdateLeaveResponse;
import com.technogise.leave_management_system.dto.UpdateLeaveRequest;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.response.SuccessResponse;
import com.technogise.leave_management_system.service.LeaveService;
import jakarta.validation.Valid;
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
            @RequestParam(name = "scope", defaultValue = "self") String scope,
            @AuthenticationPrincipal User user
    ) {
        UUID userId = user.getId();
        List<LeaveResponse> leaveList = leaveService.getAllLeaves(userId, scope, status);
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.success("Leaves retrieved successfully", leaveList));
    }

    @PostMapping
    public ResponseEntity<SuccessResponse<List<CreateLeaveResponse>>> applyLeave(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateLeaveRequest createLeaveRequest
    ) {
        UUID userId = user.getId();
        List<CreateLeaveResponse> createLeaveResponses = leaveService.applyLeave(createLeaveRequest, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(SuccessResponse.success("Leaves applied successfully", createLeaveResponses));
    }

    @GetMapping("/{leaveId}")
    public ResponseEntity<SuccessResponse<LeaveResponse>> getLeaveById(
            @AuthenticationPrincipal User user,
            @PathVariable UUID leaveId
    ) {
        UUID userId = user.getId();
        LeaveResponse leaveResponse = leaveService.getLeaveById(leaveId, userId);
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.success("Leave retrieved successfully", leaveResponse));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SuccessResponse<UpdateLeaveResponse>> updateLeave(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateLeaveRequest updateLeaveRequest,
            @AuthenticationPrincipal User user
    ) {
        UpdateLeaveResponse updateLeaveResponse = leaveService.updateLeave(
                id, updateLeaveRequest, user.getId());
        return ResponseEntity.status(HttpStatus.OK)
                .body(SuccessResponse.success("Leave updated successfully", updateLeaveResponse));
    }
}
