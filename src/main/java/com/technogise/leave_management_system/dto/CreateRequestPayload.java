package com.technogise.leave_management_system.dto;

import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.RequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateRequestPayload {

    @NotNull(message = "Request type must not be null")
    private RequestType requestType;

    private UUID leaveCategoryId;

    @NotNull(message = "Dates must not be null")
    @NotEmpty(message = "At least one date must be provided")
    private List<
        @NotNull(message = "Date in list must not be null")
                LocalDate> dates;

    @NotNull(message = "Start time must not be null")
    private LocalTime startTime;

    @NotNull(message = "Duration must not be null")
    private DurationType duration;

    @NotBlank(message = "Description must not be blank")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;
}
