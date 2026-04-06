package com.technogise.leave_management_system.repository;

import com.technogise.leave_management_system.entity.AnnualLeave;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface AnnualLeaveRepository extends JpaRepository<AnnualLeave, UUID> {

    @EntityGraph(attributePaths = {"user"})
    @Query("SELECT al FROM annual_leaves al WHERE al.year = :year")
    Page<AnnualLeave> findAllByYear(@Param("year") String year, Pageable pageable);
}
