package com.technogise.leave_management_system.repository;

import com.technogise.leave_management_system.entity.LeaveCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LeaveCategoryRepository extends JpaRepository<LeaveCategory, UUID> {
}
