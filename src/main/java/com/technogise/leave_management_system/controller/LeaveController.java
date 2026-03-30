package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.LeaveResponse;
import com.technogise.leave_management_system.response.SuccessResponse;
import com.technogise.leave_management_system.service.LeaveService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

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
            @RequestHeader(name = "user_id") UUID userId
    ) {
        List<LeaveResponse> leaveList = leaveService.getAllLeaves(userId,scope,status);
        return ResponseEntity.status(HttpStatus.OK).body(SuccessResponse.success("Leaves retrieved successfully",leaveList));
    }
}
