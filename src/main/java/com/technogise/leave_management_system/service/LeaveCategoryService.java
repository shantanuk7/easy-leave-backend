package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.LeaveCategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class LeaveCategoryService {

    private final LeaveCategoryRepository leaveCategoryRepository;

    public LeaveCategoryService(LeaveCategoryRepository leaveCategoryRepository) {
        this.leaveCategoryRepository = leaveCategoryRepository;
    }

    public LeaveCategory getLeaveCategoryById(UUID leaveCategoryId) {
        return leaveCategoryRepository.findById(leaveCategoryId)
                .orElseThrow(() -> new HttpException(HttpStatus.NOT_FOUND,"LeaveCategory not found with id: " + leaveCategoryId));
    }
}
