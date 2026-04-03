package com.technogise.leave_management_system.dto;

import lombok.Getter;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnnualLeaveBalanceResponse {
    private String employeeId;
    private String employeeName;
    private double totalLeavesAvailable;
    private double leavesTaken;
    private double leavesRemaining;
}
