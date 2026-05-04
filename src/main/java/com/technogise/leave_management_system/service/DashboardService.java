
package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.EmployeeAnnualLeaveSummaryResponse;
import com.technogise.leave_management_system.dto.EmployeeMetricsResponse;
import com.technogise.leave_management_system.entity.AnnualLeave;
import com.technogise.leave_management_system.enums.RequestStatus;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.AnnualLeaveRepository;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.RequestRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class DashboardService {
    private final UserRepository userRepository;
    private final LeaveRepository leaveRepository;
    private final AnnualLeaveRepository annualLeaveRepository;
    private final RequestRepository requestRepository;

    public DashboardService(UserRepository userRepository, LeaveRepository leaveRepository,
                            AnnualLeaveRepository annualLeaveRepository, RequestRepository requestRepository) {
        this.userRepository = userRepository;
        this.leaveRepository = leaveRepository;
        this.annualLeaveRepository = annualLeaveRepository;
        this.requestRepository = requestRepository;
    }

    public EmployeeMetricsResponse getManagerDashboardData() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        LocalDate tomorrow = today.plusDays(1);

        long totalEmployees = userRepository.count();
        long onLeaveToday = leaveRepository.countByDateAndDeletedAtIsNull(today);
        long onLeaveTomorrow = leaveRepository.countByDateAndDeletedAtIsNull(tomorrow);
        return new EmployeeMetricsResponse(totalEmployees, onLeaveToday, onLeaveTomorrow);
    }

    public EmployeeAnnualLeaveSummaryResponse getEmployeeDashboardData(UUID userId) {
        AnnualLeave annualLeave = annualLeaveRepository.findByUserId(userId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND, "User Annual Details not found with id : " + userId));

        int pendingRequests = requestRepository.countByRequestedByUserIdAndStatus(userId,RequestStatus.PENDING);

        EmployeeAnnualLeaveSummaryResponse response = new EmployeeAnnualLeaveSummaryResponse();
        response.setTotalAnnualLeaves(annualLeave.getTotal());
        response.setRemainingAnnualLeaves(annualLeave.getBalance());
        response.setLeavesTaken(annualLeave.getTaken());
        response.setPendingRequests(pendingRequests);
        return response;
    }
}

