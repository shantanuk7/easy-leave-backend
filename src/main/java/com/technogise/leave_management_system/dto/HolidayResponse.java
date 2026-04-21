package com.technogise.leave_management_system.dto;

import com.technogise.leave_management_system.enums.HolidayType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HolidayResponse {
    private UUID id;
    private String name;
    private HolidayType type;
    private LocalDate date;
}
