package com.technogise.leave_management_system.repository;

import com.technogise.leave_management_system.entity.LeaveIntegrationEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LeaveIntegrationEventRepository extends JpaRepository<LeaveIntegrationEvent, UUID> {
}
