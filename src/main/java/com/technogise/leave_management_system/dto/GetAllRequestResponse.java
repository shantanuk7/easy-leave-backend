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
public class GetAllRequestResponse {
    public UUID id;
    public String employeeName;
    public RequestType type;
    public String leaveCategory;
    public LocalDate date;
    public DurationType duration;
    public String description;
    public RequestStatus status;
    public LocalDate appliedDate;
}
