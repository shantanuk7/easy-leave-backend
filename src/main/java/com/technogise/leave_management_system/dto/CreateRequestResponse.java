package com.technogise.leave_management_system.dto;

import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.RequestStatus;
import com.technogise.leave_management_system.enums.RequestType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateRequestResponse {

    private UUID id;

    private RequestType requestType;

    private String leaveCategoryName;

    private LocalDate date;

    private LocalTime startTime;

    private DurationType duration;

    private String description;

    private RequestStatus status;
}
