package com.technogise.leave_management_system.handler;

import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.service.GoogleCalendarService;
import com.technogise.leave_management_system.service.KimaiService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LeaveIntegrationHandler {
    private final KimaiService kimaiService;
    private final GoogleCalendarService googleCalendarService;

    public LeaveIntegrationHandler(GoogleCalendarService googleCalendarService, KimaiService kimaiService) {
        this.googleCalendarService = googleCalendarService;
        this.kimaiService = kimaiService;
    }

    public void handleLeaves(List<Leave> leaves) {
        for (Leave leave : leaves) {
            googleCalendarService.syncLeave(leave);
            kimaiService.syncLeave(leave);
        }
    }
}
