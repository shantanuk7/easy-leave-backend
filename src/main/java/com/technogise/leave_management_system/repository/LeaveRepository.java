package com.technogise.leave_management_system.repository;

import com.technogise.leave_management_system.entity.Leave;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, UUID> {
    @EntityGraph(attributePaths = {"user","leaveCategory"})
    List<Leave> findAllByUserIdAndDeletedAtNull(UUID userId, Sort sort);

    List<Leave> findAllByDeletedAtIsNull(Sort sort);

    long countByDateAndDeletedAtIsNull(LocalDate date);

    @EntityGraph(attributePaths = {"user", "leaveCategory"})
    List<Leave> findAllByUserIdAndDateBetweenAndDeletedAtIsNull(UUID userId, LocalDate startDate, LocalDate endDate, Sort sort);

    long countByUserIdAndLeaveCategoryIdAndDateBetweenAndDeletedAtIsNull(
            UUID userId,
            UUID leaveCategoryId,
            LocalDate startDate,
            LocalDate endDate
    );

    Optional<Leave> findByUserIdAndDate(UUID userId, LocalDate date);

    boolean existsByUserIdAndDateAndIdNotAndDeletedAtIsNull(UUID userId, LocalDate date, UUID id);

    long countByUserIdAndHolidayIsNotNullAndDateBetweenAndDeletedAtIsNull(
            UUID userId,
            LocalDate startDate,
            LocalDate endDate
    );
}
