package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.exception.NotFoundException;
import com.technogise.leave_management_system.repository.LeaveCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveCategoryServiceTest {
    @Mock
    private LeaveCategoryRepository leaveCategoryRepository;

    @InjectMocks
    private LeaveCategoryService leaveCategoryService;

    UUID leaveCategoryId;
    @BeforeEach
    void setUp() {
        leaveCategoryId = UUID.randomUUID();
    }

    @Test
    void shouldThrowNotFoundExceptionWhenLeaveCategoryIdDoesNotExist() {
        when(leaveCategoryRepository.findById(leaveCategoryId)).
                thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> leaveCategoryService.getLeaveCategoryById(leaveCategoryId));
    }

    @Test
    void shouldReturnLeaveCategoryWhenLeaveCategoryIdExists() {
        when(leaveCategoryRepository.findById( leaveCategoryId))
                .thenReturn(Optional.of(new LeaveCategory()));

        assertInstanceOf(LeaveCategory.class,leaveCategoryService.getLeaveCategoryById(leaveCategoryId));
    }
}
