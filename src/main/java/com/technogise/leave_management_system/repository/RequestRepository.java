package com.technogise.leave_management_system.repository;

import com.technogise.leave_management_system.entity.Request;
import com.technogise.leave_management_system.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
public interface RequestRepository extends JpaRepository<Request, UUID> {

    @EntityGraph(attributePaths = {"requestedByUser", "leaveCategory"})
    Page<Request> findAllByRequestedByUserId(UUID userId, Pageable pageable);

    @EntityGraph(attributePaths = {"requestedByUser", "leaveCategory"})
    Page<Request> findAllByRequestedByUserIdAndStatus(UUID userId, RequestStatus status, Pageable pageable);
}
