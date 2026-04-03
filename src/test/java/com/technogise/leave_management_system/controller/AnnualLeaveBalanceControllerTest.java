package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.AnnualLeaveBalanceResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.response.SuccessResponse;
import com.technogise.leave_management_system.service.AnnualLeaveBalanceService;
import com.technogise.leave_management_system.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Year;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnualLeaveBalanceControllerTest {

    @Mock
    private AnnualLeaveBalanceService annualLeaveBalanceService;

    @Mock
    private UserService userService;

    @InjectMocks
    private AnnualLeaveBalanceController annualLeaveBalanceController;

    private static final int CURRENT_YEAR = Year.now().getValue();

    // -------------------------
    // Helper methods
    // -------------------------

    private User createManager() {
        User manager = new User();
        manager.setId(UUID.randomUUID());
        manager.setName("Manager");
        manager.setRole(UserRole.MANAGER);
        return manager;
    }

    private User createEmployee() {
        User employee = new User();
        employee.setId(UUID.randomUUID());
        employee.setName("Arjun");
        employee.setRole(UserRole.EMPLOYEE);
        return employee;
    }

    private AnnualLeaveBalanceResponse createLeaveBalanceResponse() {
        return new AnnualLeaveBalanceResponse(
                UUID.randomUUID().toString(),
                "Arjun",
                24.0,
                5.0,
                19.0
        );
    }

    @Test
    void shouldReturnForbiddenWhenCallerIsNotManager() {
        User employee = createEmployee();

        when(userService.getUserByUserId(employee.getId()))
                .thenReturn(employee);

        assertThrows(HttpException.class, () -> annualLeaveBalanceController.getAnnualLeaveBalance(employee.getId(), null));
    }

    @Test
    void shouldReturnEmptyDataWithNoEmployeesFoundMessageWhenNoEmployeesExist() {
        User manager = createManager();

        when(userService.getUserByUserId(manager.getId()))
                .thenReturn(manager);
        when(annualLeaveBalanceService.getAnnualLeaveBalancesForAllEmployees(CURRENT_YEAR))
                .thenReturn(List.of());

        ResponseEntity<SuccessResponse<List<AnnualLeaveBalanceResponse>>> response =
                annualLeaveBalanceController.getAnnualLeaveBalance(manager.getId(), null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().getData().isEmpty());
        assertEquals("No employees found", response.getBody().getMessage());
    }

    @Test
    void shouldReturnLeaveBalancesWhenCalledByManager() {
        User manager = createManager();
        List<AnnualLeaveBalanceResponse> leaveBalances = List.of(createLeaveBalanceResponse());

        when(userService.getUserByUserId(manager.getId()))
                .thenReturn(manager);
        when(annualLeaveBalanceService.getAnnualLeaveBalancesForAllEmployees(CURRENT_YEAR))
                .thenReturn(leaveBalances);

        ResponseEntity<SuccessResponse<List<AnnualLeaveBalanceResponse>>> response =
                annualLeaveBalanceController.getAnnualLeaveBalance(manager.getId(), null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getData().size());
        assertEquals("Employee leave balance fetched successfully", response.getBody().getMessage());
        assertTrue(response.getBody().isSuccess());
    }

    @Test
    void shouldUseYearWhenYearParamIsPassed() {
        User manager = createManager();
        List<AnnualLeaveBalanceResponse> leaveBalances = List.of(createLeaveBalanceResponse());

        when(userService.getUserByUserId(manager.getId()))
                .thenReturn(manager);
        when(annualLeaveBalanceService.getAnnualLeaveBalancesForAllEmployees(2023))
                .thenReturn(leaveBalances);

        ResponseEntity<SuccessResponse<List<AnnualLeaveBalanceResponse>>> response =
                annualLeaveBalanceController.getAnnualLeaveBalance(manager.getId(), 2023);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getData().size());
    }
}
