package com.technogise.leave_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeLeavesRecordResponse {
    private UUID leaveId;
    private String leaveType;
    private double totalLeavesAvailable;
    private double leavesTaken;
    private double leavesRemaining;
}
