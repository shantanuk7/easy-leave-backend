package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.LeaveCategoryResponse;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.LeaveCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void shouldThrowApplicationExceptionWhenLeaveCategoryIdDoesNotExist() {
        when(leaveCategoryRepository.findById(leaveCategoryId)).
                thenReturn(Optional.empty());

        assertThrows(HttpException.class, () -> leaveCategoryService.getLeaveCategoryById(leaveCategoryId));
    }

    @Test
    void shouldReturnLeaveCategoryWhenLeaveCategoryIdExists() {
        when(leaveCategoryRepository.findById( leaveCategoryId))
                .thenReturn(Optional.of(new LeaveCategory()));

        assertInstanceOf(LeaveCategory.class,leaveCategoryService.getLeaveCategoryById(leaveCategoryId));
    }

    @Test
    void shouldReturnAllLeaveCategories(){
        // Given
        UUID id = leaveCategoryId;
        String categoryName = "Annual Leave";

        LeaveCategory leaveCategory = new LeaveCategory();
        leaveCategory.setId(id);
        leaveCategory.setName(categoryName);

        LeaveCategoryResponse leaveCategoryResponse = new LeaveCategoryResponse();
        leaveCategoryResponse.setId(id);
        leaveCategoryResponse.setName(categoryName);

        when(leaveCategoryRepository.findAll()).thenReturn(List.of(leaveCategory));

        List<LeaveCategoryResponse> mockResponse = List.of(leaveCategoryResponse);

        // When
        List<LeaveCategoryResponse> actualResponse = leaveCategoryService.getAllLeaveCategories();

        // Then
        assertEquals(mockResponse.size(), actualResponse.size());
        assertEquals(mockResponse.getFirst().getId(), actualResponse.getFirst().getId());
        assertEquals(mockResponse.getFirst().getName(), actualResponse.getFirst().getName());
    }
}
