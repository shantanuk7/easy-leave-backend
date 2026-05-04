package com.technogise.leave_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeAnnualLeaveSummaryResponse {
    private double totalAnnualLeaves;
    private double remainingAnnualLeaves;
    private double leavesTaken;
    private int pendingRequests;
}
