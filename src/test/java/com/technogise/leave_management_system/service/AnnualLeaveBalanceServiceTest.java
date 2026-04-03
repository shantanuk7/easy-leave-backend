package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.AnnualLeaveBalanceResponse;
import com.technogise.leave_management_system.entity.AnnualLeave;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.repository.AnnualLeaveRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnualLeaveBalanceServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private AnnualLeaveRepository annualLeaveRepository;

    @InjectMocks
    private AnnualLeaveBalanceService annualLeaveBalanceService;

    private static final int CURRENT_YEAR = Year.now().getValue();

    private User createEmployee() {
        User employee = new User();
        employee.setId(UUID.randomUUID());
        employee.setName("Arjun");
        employee.setRole(UserRole.EMPLOYEE);
        return employee;
    }

    private AnnualLeave createAnnualLeave(User employee, String total, String taken, String balance, int year) {
        AnnualLeave annualLeave = new AnnualLeave();
        annualLeave.setId(UUID.randomUUID());
        annualLeave.setUser(employee);
        annualLeave.setTotal(total);
        annualLeave.setTaken(taken);
        annualLeave.setBalance(balance);
        annualLeave.setYear(String.valueOf(year));
        annualLeave.setCreatedAt(LocalDateTime.now());
        annualLeave.setUpdatedAt(LocalDateTime.now());
        return annualLeave;
    }

    @Test
    void shouldReturnLeaveBalanceForEmployeeWithLeavesTaken() {
        User employee = createEmployee();
        AnnualLeave annualLeave = createAnnualLeave(employee, "24", "5", "19", CURRENT_YEAR);

        when(userService.getAllEmployees()).thenReturn(List.of(employee));
        when(annualLeaveRepository.findByUserIdAndYear(employee.getId(), String.valueOf(CURRENT_YEAR)))
                .thenReturn(Optional.of(annualLeave));

        List<AnnualLeaveBalanceResponse> result = annualLeaveBalanceService.getAnnualLeaveBalancesForAllEmployees(CURRENT_YEAR);

        assertEquals(1, result.size());
        assertEquals(employee.getId().toString(), result.getFirst().getEmployeeId());
        assertEquals("Arjun", result.getFirst().getEmployeeName());
        assertEquals(24.0, result.getFirst().getTotalLeavesAvailable());
        assertEquals(5.0, result.getFirst().getLeavesTaken());
        assertEquals(19.0, result.getFirst().getLeavesRemaining());
    }

    @Test
    void shouldReturnFullBalanceWhenEmployeeHasNotTakenAnyLeave() {
        User employee = createEmployee();
        AnnualLeave annualLeave = createAnnualLeave(employee, "24", "0", "24", CURRENT_YEAR);

        when(userService.getAllEmployees()).thenReturn(List.of(employee));
        when(annualLeaveRepository.findByUserIdAndYear(employee.getId(), String.valueOf(CURRENT_YEAR)))
                .thenReturn(Optional.of(annualLeave));

        List<AnnualLeaveBalanceResponse> result = annualLeaveBalanceService.getAnnualLeaveBalancesForAllEmployees(CURRENT_YEAR);

        assertEquals(1, result.size());
        assertEquals(24.0, result.getFirst().getTotalLeavesAvailable());
        assertEquals(0.0, result.getFirst().getLeavesTaken());
        assertEquals(24.0, result.getFirst().getLeavesRemaining());
    }

    @Test
    void shouldReturnEmptyListWhenNoEmployeesExist() {
        when(userService.getAllEmployees()).thenReturn(List.of());

        List<AnnualLeaveBalanceResponse> result = annualLeaveBalanceService.getAnnualLeaveBalancesForAllEmployees(CURRENT_YEAR);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotIncludeEmployeeWhenNoAnnualLeaveRecordExistsForRequestedYear() {
        User employee = createEmployee();

        when(userService.getAllEmployees()).thenReturn(List.of(employee));
        when(annualLeaveRepository.findByUserIdAndYear(employee.getId(), "2022")).thenReturn(Optional.empty());

        List<AnnualLeaveBalanceResponse> result = annualLeaveBalanceService.getAnnualLeaveBalancesForAllEmployees(2022);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldDefaultToCurrentYearWhenNoYearParamProvided() {
        User employee = createEmployee();
        AnnualLeave annualLeave = createAnnualLeave(employee, "24", "3", "21", CURRENT_YEAR);

        when(userService.getAllEmployees()).thenReturn(List.of(employee));
        when(annualLeaveRepository.findByUserIdAndYear(employee.getId(), String.valueOf(CURRENT_YEAR)))
                .thenReturn(Optional.of(annualLeave));

        List<AnnualLeaveBalanceResponse> result = annualLeaveBalanceService.getAnnualLeaveBalancesForAllEmployees(CURRENT_YEAR);

        assertEquals(1, result.size());
        assertEquals(21.0, result.getFirst().getLeavesRemaining());
    }
}
