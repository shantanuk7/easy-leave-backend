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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnnualLeaveBalanceServiceTest {

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
        Page<AnnualLeave> annualLeavePage = new PageImpl<>(List.of(annualLeave));

        when(annualLeaveRepository.findAllByYear(String.valueOf(CURRENT_YEAR), Pageable.unpaged())).thenReturn(annualLeavePage);

        Page<AnnualLeaveBalanceResponse> result = annualLeaveBalanceService
                .getAnnualLeaveBalancesForAllEmployees(CURRENT_YEAR, Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals(employee.getId().toString(), result.getContent().getFirst().getEmployeeId());
        assertEquals("Arjun", result.getContent().getFirst().getEmployeeName());
        assertEquals(24.0, result.getContent().getFirst().getTotalLeavesAvailable());
        assertEquals(5.0, result.getContent().getFirst().getLeavesTaken());
        assertEquals(19.0, result.getContent().getFirst().getLeavesRemaining());
    }

    @Test
    void shouldReturnFullBalanceWhenEmployeeHasNotTakenAnyLeave() {
        User employee = createEmployee();
        AnnualLeave annualLeave = createAnnualLeave(employee, "24", "0", "24", CURRENT_YEAR);
        Page<AnnualLeave> annualLeavePage = new PageImpl<>(List.of(annualLeave));

        when(annualLeaveRepository.findAllByYear(String.valueOf(CURRENT_YEAR), Pageable.unpaged())).thenReturn(annualLeavePage);

        Page<AnnualLeaveBalanceResponse> result = annualLeaveBalanceService
                .getAnnualLeaveBalancesForAllEmployees(CURRENT_YEAR, Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals(24.0, result.getContent().getFirst().getTotalLeavesAvailable());
        assertEquals(0.0, result.getContent().getFirst().getLeavesTaken());
        assertEquals(24.0, result.getContent().getFirst().getLeavesRemaining());
    }

    @Test
    void shouldReturnEmptyPageWhenNoAnnualLeaveRecordsExist() {
        when(annualLeaveRepository.findAllByYear(String.valueOf(CURRENT_YEAR), Pageable.unpaged())).thenReturn(Page.empty());

        Page<AnnualLeaveBalanceResponse> result = annualLeaveBalanceService
                .getAnnualLeaveBalancesForAllEmployees(CURRENT_YEAR, Pageable.unpaged());

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldNotIncludeEmployeeWhenNoAnnualLeaveRecordExistsForRequestedYear() {
        when(annualLeaveRepository.findAllByYear("2022", Pageable.unpaged())).thenReturn(Page.empty());

        Page<AnnualLeaveBalanceResponse> result = annualLeaveBalanceService
                .getAnnualLeaveBalancesForAllEmployees(2022, Pageable.unpaged());

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnCorrectBalanceWhenYearParamIsPassed() {
        User employee = createEmployee();
        AnnualLeave annualLeave = createAnnualLeave(employee, "24", "3", "21", CURRENT_YEAR);
        Page<AnnualLeave> annualLeavePage = new PageImpl<>(List.of(annualLeave));

        when(annualLeaveRepository.findAllByYear(String.valueOf(CURRENT_YEAR), Pageable.unpaged())).thenReturn(annualLeavePage);

        Page<AnnualLeaveBalanceResponse> result = annualLeaveBalanceService
                .getAnnualLeaveBalancesForAllEmployees(CURRENT_YEAR, Pageable.unpaged());

        assertEquals(1, result.getContent().size());
        assertEquals(21.0, result.getContent().getFirst().getLeavesRemaining());
    }
}
