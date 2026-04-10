package com.technogise.leave_management_system.controller;
import com.technogise.leave_management_system.dto.LeaveCategoryResponse;
import com.technogise.leave_management_system.response.SuccessResponse;
import com.technogise.leave_management_system.service.LeaveCategoryService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/leave-categories")
@AllArgsConstructor
public class LeaveCategoryController {

    private final LeaveCategoryService leaveCategoryService;

    @GetMapping
    public ResponseEntity<SuccessResponse<List<LeaveCategoryResponse>>> getAll() {
        log.info("GET /api/leave-categories called");

        List<LeaveCategoryResponse> leaveCategories = leaveCategoryService.getAllLeaveCategories();
        log.debug("Returning {} leave categories", leaveCategories.size());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(
                        SuccessResponse.success("Leave Categories retrieved successfully", leaveCategories)
                );
    }
}
