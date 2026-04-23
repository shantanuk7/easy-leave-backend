package com.technogise.leave_management_system.repository;

import com.technogise.leave_management_system.entity.Holiday;
import com.technogise.leave_management_system.enums.HolidayType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, UUID> {
    boolean existsByNameIgnoreCaseAndDateBetween(String name, LocalDate startDate, LocalDate endDate);
    boolean existsByDate(LocalDate date);
    List<Holiday> findAllByType(HolidayType type);
}
