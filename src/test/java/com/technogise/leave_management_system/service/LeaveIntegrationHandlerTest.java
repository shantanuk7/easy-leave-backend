package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.entity.Leave;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveIntegrationHandlerTest {

    @Mock
    private GoogleCalendarService googleCalendarService;

    @InjectMocks
    private LeaveIntegrationHandler leaveIntegrationHandler;

    @Test
    void shouldCallSyncLeaveForEachLeaveInList() {
        Leave leave1 = new Leave();
        Leave leave2 = new Leave();

        leaveIntegrationHandler.handleLeaves(List.of(leave1, leave2));

        verify(googleCalendarService).syncLeave(leave1);
        verify(googleCalendarService).syncLeave(leave2);
    }
}
