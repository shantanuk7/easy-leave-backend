package com.technogise.leave_management_system.dto;

import com.technogise.leave_management_system.enums.DurationType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateLeaveResponse {

    private UUID id;

    private LocalDate date;

    private String leaveCategoryName;

    private DurationType duration;

    private LocalTime startTime;

    private String description;
}