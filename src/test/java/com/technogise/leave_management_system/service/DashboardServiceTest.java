package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.EmployeeMetricsResponse;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeaveRepository leaveRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void shouldReturnCorrectDashboardData() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        when(userRepository.count()).thenReturn(50L);
        when(leaveRepository.countByDate(today)).thenReturn(10L);
        when(leaveRepository.countByDate(tomorrow)).thenReturn(5L);
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
        when(leaveRepository.countByDate(today)).thenReturn(300L);
        when(leaveRepository.countByDate(tomorrow)).thenReturn(250L);

        EmployeeMetricsResponse response = dashboardService.getManagerDashboardData();

        assertEquals(1000L, response.getTotalEmployees());
        assertEquals(300L, response.getTotalEmployeesOnLeaveToday());
        assertEquals(250L, response.getTotalEmployeesOnLeaveTomorrow());
    }
}
