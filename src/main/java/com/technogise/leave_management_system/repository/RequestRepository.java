package com.technogise.leave_management_system.repository;

import com.technogise.leave_management_system.entity.Request;
import com.technogise.leave_management_system.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface RequestRepository extends JpaRepository<Request, UUID> {
    boolean existsByRequestedByUserIdAndDateAndStatusIn(
            UUID requestedByUserId, LocalDate date, List<RequestStatus> statuses);
}
