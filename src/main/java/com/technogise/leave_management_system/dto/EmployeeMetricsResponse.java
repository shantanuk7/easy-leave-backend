package com.technogise.leave_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeMetricsResponse {
    private long totalEmployees;
    private long totalEmployeesOnLeaveToday;
    private long totalEmployeesOnLeaveTomorrow;
}
