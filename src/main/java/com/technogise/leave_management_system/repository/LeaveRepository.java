package com.technogise.leave_management_system.repository;

import com.technogise.leave_management_system.entity.Leave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeaveRepository extends JpaRepository<Leave, UUID>, JpaSpecificationExecutor<Leave> {
    @EntityGraph(attributePaths = {"user", "leaveCategory"})
    Page<Leave> findAll(Specification<Leave> spec, Pageable pageable);

    List<Leave> findAllByUserIdAndDeletedAtNull(UUID userId);

    long countByDateAndDeletedAtIsNull(LocalDate date);

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
