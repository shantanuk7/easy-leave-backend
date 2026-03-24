package com.technogise.leave_management_system.dto;

import com.technogise.leave_management_system.enums.DurationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeaveResponse {
    public UUID id;
    public Date date;
    public String employeeName;
    public String type;
    public DurationType duration;
    public LocalTime startTime;
    public LocalDateTime applyOn;
    public String reason;
}
