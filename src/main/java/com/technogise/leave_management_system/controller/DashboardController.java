package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.EmployeeMetricsResponse;
import com.technogise.leave_management_system.response.SuccessResponse;
import com.technogise.leave_management_system.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}


