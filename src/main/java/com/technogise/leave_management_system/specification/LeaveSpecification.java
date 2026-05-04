package com.technogise.leave_management_system.specification;

import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.exception.HttpException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.UUID;

public class LeaveSpecification {
    public static Specification<Leave> notDeleted() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isNull(root.get("deletedAt"));
    }

    public static Specification<Leave> allLeavesOfEmployee(UUID userId) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.equal(root.get("user").get("id"), userId);
    }

    public static Specification<Leave> leavesByStatus(String status) {
        return (root, query, criteriaBuilder) -> {
            LocalDate today = LocalDate.now();
            return switch (status.toUpperCase()) {
                case "UPCOMING" -> criteriaBuilder.greaterThan(root.get("date"), today);
                case "COMPLETED" -> criteriaBuilder.lessThan(root.get("date"), today);
                case "ONGOING" -> criteriaBuilder.equal(root.get("date"), today);
                default -> throw new HttpException(HttpStatus.BAD_REQUEST, "Invalid status query parameter");
            };
        };
    }

    public static Specification<Leave> leavesWithinYear(int year) {
        return (root, query, criteriaBuilder) ->
            criteriaBuilder.between(root.get("date"),
                LocalDate.of(year, 1, 1),
                LocalDate.of(year, 12, 31)
            );
    }

    public static Specification<Leave> noFilter() {
        return (root, query, cb) -> cb.conjunction();
    }
}
