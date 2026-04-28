package com.technogise.leave_management_system.handler;

import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.service.GoogleCalendarService;
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
