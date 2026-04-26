package com.technogise.leave_management_system.dto;

import com.technogise.leave_management_system.enums.DurationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateLeaveResponse {

    private UUID id;

    private LocalDate date;

    private String type;

    private DurationType duration;

    private LocalTime startTime;

    private String description;

}
