package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.LeaveCategoryResponse;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.repository.LeaveCategoryRepository;
import com.technogise.leave_management_system.service.LeaveCategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LeaveCategoryController.class)
public class LeaveCategoryControllerTest {

    @MockitoBean
    private LeaveCategoryService leaveCategoryService;

    @MockitoBean
    private LeaveCategoryRepository leaveCategoryRepository;

    @Autowired
    private MockMvc mockMvc;

    LeaveCategory leaveCategory = new LeaveCategory();

    @BeforeEach
    void setup() {
        leaveCategory.setId(UUID.randomUUID());
        leaveCategory.setName("Annual Leave");
    }

    @Test
    void shouldRespondWithAllLeaveCategories_And200Status() throws Exception {
        List<LeaveCategoryResponse> response = List.of(
                new LeaveCategoryResponse(
                        leaveCategory.getId(),
                        leaveCategory.getName()
                )
        );

        when(leaveCategoryService.getAllLeaveCategories()).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/leave-categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data[0].id").value(leaveCategory.getId().toString()))
                .andExpect(jsonPath("$.data[0].name").value(leaveCategory.getName()));
    }

    @Test
    void shouldReturnEmptyList_And200Status_whenNoLeaveCategoriesExist() throws Exception {
        List<LeaveCategoryResponse> response = List.of();

        when(leaveCategoryService.getAllLeaveCategories()).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/leave-categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data").isEmpty());
    }
}