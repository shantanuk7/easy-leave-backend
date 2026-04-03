package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.AnnualLeaveBalanceResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.response.SuccessResponse;
import com.technogise.leave_management_system.service.AnnualLeaveBalanceService;
import com.technogise.leave_management_system.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/annual-leaves")
public class AnnualLeaveBalanceController {

    private final AnnualLeaveBalanceService annualLeaveBalanceService;
    private final UserService userService;

    public AnnualLeaveBalanceController(AnnualLeaveBalanceService annualLeaveBalanceService, UserService userService) {
        this.annualLeaveBalanceService = annualLeaveBalanceService;
        this.userService = userService;
    }
    @GetMapping
    public ResponseEntity<SuccessResponse<List<AnnualLeaveBalanceResponse>>> getAnnualLeaveBalance(
            @RequestHeader(name = "user_id") UUID userId, @RequestParam(name = "year", required = false) Integer year) {

        int requestedYear = (year != null) ? year : Year.now().getValue();

        User user = userService.getUserByUserId(userId);
        if (user.getRole() != UserRole.MANAGER) {
            throw new HttpException(HttpStatus.FORBIDDEN, "Access only allowed to managers");
        }

        List<AnnualLeaveBalanceResponse> annualLeaveBalancesList = annualLeaveBalanceService.getAnnualLeaveBalancesForAllEmployees(requestedYear);

        String message = annualLeaveBalancesList.isEmpty() ? "No employees found" : "Employee leave balance fetched successfully";

        return ResponseEntity.status(HttpStatus.OK).body(SuccessResponse.success(message, annualLeaveBalancesList));
    }
}
