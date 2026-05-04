package com.technogise.leave_management_system.repository;

import com.technogise.leave_management_system.entity.LeaveIntegrationEvent;
import com.technogise.leave_management_system.enums.PlatformType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LeaveIntegrationEventRepository extends JpaRepository<LeaveIntegrationEvent, UUID> {
    Optional<LeaveIntegrationEvent> findByLeaveIdAndPlatformAndDeletedAtIsNull(UUID leaveId, PlatformType platform);
}
