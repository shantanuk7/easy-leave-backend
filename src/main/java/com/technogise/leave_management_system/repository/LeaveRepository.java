package com.technogise.leave_management_system.repository;

import com.technogise.leave_management_system.entity.Leave;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, UUID> {
    @EntityGraph(attributePaths = {"user","leaveCategory"})
    List<Leave> findAllByUserId(UUID userId, Sort sort);

    boolean existsByUserIdAndDateAndIdNot(UUID userId, LocalDate date, UUID excludeLeaveId);

    long countByDate(LocalDate date);

    @EntityGraph(attributePaths = {"user", "leaveCategory"})
    List<Leave> findAllByUserIdAndDateBetween(UUID userId, LocalDate startDate, LocalDate endDate, Sort sort);

    long countByDate(LocalDate date);
}
