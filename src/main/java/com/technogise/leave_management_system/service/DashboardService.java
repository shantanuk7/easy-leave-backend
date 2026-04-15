
package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.EmployeeMetricsResponse;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class DashboardService {
    private final UserRepository userRepository;
    private final LeaveRepository leaveRepository;

    public DashboardService(UserRepository userRepository, LeaveRepository leaveRepository) {
        this.userRepository = userRepository;
        this.leaveRepository = leaveRepository;
    }

    public EmployeeMetricsResponse getManagerDashboardData() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate tomorrow = today.plusDays(1);

        long totalEmployees = userRepository.count();
        long onLeaveToday = leaveRepository.countByDate(today);
        long onLeaveTomorrow = leaveRepository.countByDate(tomorrow);
        return new EmployeeMetricsResponse(totalEmployees, onLeaveToday, onLeaveTomorrow);
    }
}

