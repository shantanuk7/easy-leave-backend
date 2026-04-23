package com.technogise.leave_management_system.dto;

import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.RequestStatus;
import com.technogise.leave_management_system.enums.RequestType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RequestResponse {
    private UUID id;
    private String employeeName;
    private RequestType type;
    private String leaveCategory;
    private LocalDate date;
    private DurationType duration;
    private String description;
    private RequestStatus status;
    private LocalDate appliedDate;
}
