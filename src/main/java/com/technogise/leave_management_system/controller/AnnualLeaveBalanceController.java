package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.AnnualLeaveBalanceResponse;
import com.technogise.leave_management_system.response.SuccessResponse;
import com.technogise.leave_management_system.service.AnnualLeaveBalanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.Year;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/annual-leaves")
public class AnnualLeaveBalanceController {

    private final AnnualLeaveBalanceService annualLeaveBalanceService;

    public AnnualLeaveBalanceController(AnnualLeaveBalanceService annualLeaveBalanceService) {
        this.annualLeaveBalanceService = annualLeaveBalanceService;
    }
    @GetMapping
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<SuccessResponse<Page<AnnualLeaveBalanceResponse>>> getAnnualLeaveBalance(
               @RequestParam(name = "year", required = false) Integer year, Pageable pageable) {

        log.info("GET /api/annual-leaves called ");

        int requestedYear = (year != null) ? year : Year.now().getValue();
        Page<AnnualLeaveBalanceResponse> annualLeaveBalancesPage = annualLeaveBalanceService.getAnnualLeaveBalancesForAllEmployees(
                        requestedYear, pageable);

        log.debug("Returning employee leave balances");

        String message = annualLeaveBalancesPage.isEmpty() ? "No employee leave balance found" : "Employee leave balance fetched successfully";

        return ResponseEntity.status(HttpStatus.OK).body(SuccessResponse.success(message, annualLeaveBalancesPage));
    }
    @GetMapping("/years")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<SuccessResponse<List<String>>> getDistinctYears() {

        log.info("GET /api/annual-leaves/years called");

        List<String> years = annualLeaveBalanceService.getDistinctYears();

        log.debug("Returning {} distinct years", years.size());
        String message = years.isEmpty() ? "No years found" : "Years fetched successfully";

        return ResponseEntity.status(HttpStatus.OK).body(SuccessResponse.success(message, years));
    }
}
