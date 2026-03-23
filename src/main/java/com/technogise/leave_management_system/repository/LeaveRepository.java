package com.technogise.leave_management_system.repository;

import com.technogise.leave_management_system.entity.Leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface LeaveRepository extends JpaRepository<Leave, UUID> {

}
