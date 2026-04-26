package com.technogise.leave_management_system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class KimaiCreateLeaveRequest {
    private String begin;
    private String end;
    private int project;
    private int activity;
    private String description;
    private int user;
}
