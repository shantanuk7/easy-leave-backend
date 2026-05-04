package com.technogise.leave_management_system.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LeaveFilterRequest {
    private String scope;
    private String status;
    private UUID empId;
    private Integer year;
}
