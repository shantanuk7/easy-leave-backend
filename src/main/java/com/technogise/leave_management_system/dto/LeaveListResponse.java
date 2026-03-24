package com.technogise.leave_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LeaveListResponse {
    public UUID id;
    public Date date;
    public String type;
    public String duration;
    public String reason;
}
