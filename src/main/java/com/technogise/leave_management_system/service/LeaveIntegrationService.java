package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.entity.Leave;

public interface LeaveIntegrationService {
    void syncLeave(Leave leave);
}
