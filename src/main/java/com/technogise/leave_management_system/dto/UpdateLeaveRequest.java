package com.technogise.leave_management_system.dto;

import com.technogise.leave_management_system.enums.DurationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotNull(message = "Date must not be null")
    private LocalDate date;

    @NotNull(message = "Start time must not be null")
    private LocalTime startTime;

    @NotBlank(message = "Description must not be blank")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Duration must not be null")
    private DurationType duration;

    @NotNull(message = "leaveCategoryId must not be null")
    private UUID leaveCategoryId;
}
