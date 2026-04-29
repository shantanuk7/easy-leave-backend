package com.technogise.leave_management_system.dto;

import com.technogise.leave_management_system.enums.DurationType;
import jakarta.validation.constraints.Size;
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
public class UpdateLeaveRequest {

    private LocalDate date;

    private LocalTime startTime;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    private DurationType duration;

    private UUID leaveCategoryId;

    private UUID holidayId;
}
