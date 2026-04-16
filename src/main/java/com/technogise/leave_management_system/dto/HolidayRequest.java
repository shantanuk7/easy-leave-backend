package com.technogise.leave_management_system.dto;

import com.technogise.leave_management_system.enums.HolidayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayRequest {
    private String name;
    private HolidayType type;
    private LocalDate date;
}
