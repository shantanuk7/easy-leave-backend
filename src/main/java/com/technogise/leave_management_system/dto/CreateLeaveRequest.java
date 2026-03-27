package com.technogise.leave_management_system.dto;

import com.technogise.leave_management_system.enums.DurationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LeaveRequest  {

    private List<LocalDate> dates;

    private UUID leaveCategoryId;

    private DurationType duration;

    private LocalTime startTime;

    private String description;
}
