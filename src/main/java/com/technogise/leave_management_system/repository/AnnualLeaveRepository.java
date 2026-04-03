package com.technogise.leave_management_system.repository;

import com.technogise.leave_management_system.entity.AnnualLeave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnnualLeaveRepository extends JpaRepository<AnnualLeave, UUID> {

    @Query("SELECT al FROM annual_leaves al WHERE al.user.id = :userId AND al.year = :year")
    Optional<AnnualLeave> findByUserIdAndYear(@Param("userId") UUID userId, @Param("year") String year);
}
