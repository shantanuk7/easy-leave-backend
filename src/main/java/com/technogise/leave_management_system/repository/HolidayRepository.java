package com.technogise.leave_management_system.repository;

import com.technogise.leave_management_system.entity.Holiday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface HolidayRepository extends JpaRepository<Holiday, UUID> {

}
