package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.EmployeeAnnualLeaveSummaryResponse;
import com.technogise.leave_management_system.dto.EmployeeMetricsResponse;
import com.technogise.leave_management_system.entity.AnnualLeave;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.RequestStatus;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.AnnualLeaveRepository;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.RequestRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeaveRepository leaveRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Mock
    private AnnualLeaveRepository annualLeaveRepository;

    @Mock
    private RequestRepository requestRepository;

    @Test
    void shouldReturnCorrectDashboardData() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        when(userRepository.count()).thenReturn(50L);
        when(leaveRepository.countByDateAndDeletedAtIsNull(today)).thenReturn(10L);
        when(leaveRepository.countByDateAndDeletedAtIsNull(tomorrow)).thenReturn(5L);
        EmployeeMetricsResponse response = dashboardService.getManagerDashboardData();
        assertEquals(50L, response.getTotalEmployees());
        assertEquals(10L, response.getTotalEmployeesOnLeaveToday());
        assertEquals(5L, response.getTotalEmployeesOnLeaveTomorrow());
    }

    @Test
    void shouldHandleLargeNumbersCorrectly() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        when(userRepository.count()).thenReturn(1000L);
        when(leaveRepository.countByDateAndDeletedAtIsNull(today)).thenReturn(300L);
        when(leaveRepository.countByDateAndDeletedAtIsNull(tomorrow)).thenReturn(250L);

        EmployeeMetricsResponse response = dashboardService.getManagerDashboardData();

        assertEquals(1000L, response.getTotalEmployees());
        assertEquals(300L, response.getTotalEmployeesOnLeaveToday());
        assertEquals(250L, response.getTotalEmployeesOnLeaveTomorrow());
    }

    @Test
    void shouldReturnEmployeeDashboardData() {
        User user = new User();
        UUID userId = UUID.randomUUID();
        user.setId(userId);
        AnnualLeave annualLeave = new AnnualLeave();
        annualLeave.setTotal(24);
        annualLeave.setBalance(12);
        annualLeave.setTaken(12);
        when(annualLeaveRepository.findByUserId(userId)).thenReturn(Optional.of(annualLeave));
        when(requestRepository.countByRequestedByUserIdAndStatus(userId, RequestStatus.PENDING)).thenReturn(12);
        EmployeeAnnualLeaveSummaryResponse response =  dashboardService.getEmployeeDashboardData(userId);
        assertEquals(response.getTotalAnnualLeaves(),annualLeave.getTotal());
    }

    @Test
    void shouldThrowUserAnnualLeaveNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(annualLeaveRepository.findByUserId(userId)).thenReturn(Optional.empty());

        HttpException exception = assertThrows(
                HttpException.class,
                () -> dashboardService.getEmployeeDashboardData(userId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
