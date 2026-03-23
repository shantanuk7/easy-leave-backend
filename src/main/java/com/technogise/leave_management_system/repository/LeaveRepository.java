package com.technogise.leave_management_system.repository;

import com.technogise.leave_management_system.entity.Leave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LeaveRepository extends JpaRepository<Leave, UUID> {

}
