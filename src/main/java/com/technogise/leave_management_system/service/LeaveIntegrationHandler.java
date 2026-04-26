package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.entity.Leave;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaveIntegrationHandler {

    private final GoogleCalendarService googleCalendarService;

    public LeaveIntegrationHandler(GoogleCalendarService googleCalendarService) {
        this.googleCalendarService = googleCalendarService;
    }

    public void handleLeaves(List<Leave> leaves) {
        for (Leave leave : leaves) {
            googleCalendarService.syncLeave(leave);
        }
    }
}
