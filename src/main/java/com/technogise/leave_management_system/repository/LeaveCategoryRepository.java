package com.technogise.leave_management_system.repository;

import com.technogise.leave_management_system.entity.LeaveCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveCategoryRepository extends JpaRepository<LeaveCategory, UUID> {
    Optional<LeaveCategory> findByName(String name);
}
