package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.EmployeeAnnualLeaveSummaryResponse;
import com.technogise.leave_management_system.dto.EmployeeMetricsResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.response.SuccessResponse;
import com.technogise.leave_management_system.service.DashboardService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    public DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<SuccessResponse<EmployeeMetricsResponse>> getManagerDashboard() {
        EmployeeMetricsResponse dashboardResponse = dashboardService.getManagerDashboardData();
        return ResponseEntity.ok(
                SuccessResponse.success("Manager Dashboard data retrieved successfully", dashboardResponse)
        );
    }

    @GetMapping("/employee")
    public ResponseEntity<SuccessResponse<EmployeeAnnualLeaveSummaryResponse>> getEmployeeDashboardMetrics(
            @AuthenticationPrincipal User user
    ) {
        log.info("GET /api/dashboard/employee called by userId={}", user.getId());

        EmployeeAnnualLeaveSummaryResponse dashboardResponse = dashboardService.getEmployeeDashboardData(user.getId());
        log.debug("Returning dashboard info for userId={}, dashboard data = {}", user.getId(),dashboardResponse);

        return ResponseEntity.ok(
                SuccessResponse.success("Employee Annual Leave data retrieved successfully", dashboardResponse)
        );
    }
}


