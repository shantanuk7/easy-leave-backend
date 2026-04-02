package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.UserResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @MockitoBean
    private UserService userService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private User employee;
    private User admin;

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setId(UUID.randomUUID());
        employee.setName("Priyansh Saxena");
        employee.setRole(UserRole.EMPLOYEE);
        employee.setEmail("priyansh@gmail.com");

        admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setName("ADMIN");
        admin.setRole(UserRole.ADMIN);
        admin.setEmail("admin@gmail.com");
    }

    @Test
    void shouldReturn200WithListOfAllEmployeesWhenManagerOrAdminRequestsDetails() throws Exception {
        List<UserResponse> responses = List.of(
                new UserResponse(employee.getId(), employee.getEmail(), employee.getName(), employee.getRole()),
                new UserResponse(admin.getId(), admin.getEmail(), admin.getName(), admin.getRole())
        );

        when(userService.getAllUsers(admin.getId())).thenReturn(responses);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/users")
                        .header("user_id", admin.getId()))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Users retrieved successfully"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].id").value(responses.get(0).getId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].name").value(responses.get(0).getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].id").value(responses.get(1).getId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].name").value(responses.get(1).getName()));
    }
}