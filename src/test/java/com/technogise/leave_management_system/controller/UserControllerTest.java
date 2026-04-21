package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.UpdateUserRoleRequest;
import com.technogise.leave_management_system.dto.EmployeeLeavesRecordResponse;
import com.technogise.leave_management_system.dto.UserResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.repository.UserRepository;
import com.technogise.leave_management_system.service.JwtService;
import com.technogise.leave_management_system.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@AutoConfigureMockMvc
@WebMvcTest(value = UserController.class, excludeAutoConfiguration = {
    OAuth2ClientAutoConfiguration.class,
    OAuth2ClientWebSecurityAutoConfiguration.class
})
@EnableMethodSecurity
class UserControllerTest {

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    private User employee;
    private User admin;
    private User manager;

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setId(UUID.randomUUID());
        employee.setName("Priyansh Saxena");
        employee.setRole(UserRole.EMPLOYEE);
        employee.setEmail("priyansh@technogise.com");

        manager = new User();
        manager.setId(UUID.randomUUID());
        manager.setName("Raj");
        manager.setRole(UserRole.MANAGER);
        manager.setEmail("raj@technogise.com");

        admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setName("ADMIN");
        admin.setRole(UserRole.ADMIN);
        admin.setEmail("admin@technogise.com");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200WithListOfAllUsersWhenManagerOrAdminRequests() throws Exception {
        List<UserResponse> responses = List.of(
                new UserResponse(employee.getId(), employee.getEmail(), employee.getName(), employee.getRole()),
                new UserResponse(admin.getId(), admin.getEmail(), admin.getName(), admin.getRole())
        );

        Page<UserResponse> page = new PageImpl<>(responses);
        when(userService.getAllUsers(any(Pageable.class)))
                .thenReturn(page);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/users")
                        .param("page", "0")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Users retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].id").value(responses.get(0).getId().toString()))
                .andExpect(jsonPath("$.data.content[0].name").value(responses.get(0).getName()))
                .andExpect(jsonPath("$.data.content[1].id").value(responses.get(1).getId().toString()))
                .andExpect(jsonPath("$.data.content[1].name").value(responses.get(1).getName()));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnSuccessWhenRoleUpdated() throws Exception {
        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest(employee.getId(), UserRole.MANAGER);
        doNothing().when(userService)
                .updateRole(any(UUID.class), any(UpdateUserRoleRequest.class));
        mockMvc.perform(MockMvcRequestBuilders
                        .patch("/api/users/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void shouldReturn200AndLeaveBalanceOfSingleEmployee() throws Exception {
        List<EmployeeLeavesRecordResponse> mockResponse = List.of(
                EmployeeLeavesRecordResponse.builder()
                        .leaveId(UUID.randomUUID())
                        .leaveType("Annual")
                        .leavesTaken(4)
                        .totalLeavesAvailable(24)
                        .leavesRemaining(20)
                        .build()
        );

        when(userService.getEmployeeLeavesRecordByYear(employee.getId(), 2026))
                .thenReturn(mockResponse);

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/users/{userId}/leave-balance", employee.getId())
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message")
                        .value("Employee leaves record retrieved successfully"))
                .andExpect(jsonPath("$.data[0].leaveType").value("Annual"))
                .andExpect(jsonPath("$.data[0].leavesTaken").value(4))
                .andExpect(jsonPath("$.data[0].leavesRemaining").value(20));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnEmptyListWhenNoLeavesExist() throws Exception {
        when(userService.getEmployeeLeavesRecordByYear(employee.getId(), 2026))
                .thenReturn(List.of());

        mockMvc.perform(MockMvcRequestBuilders
                        .get("/api/users/{userId}/leave-balance", employee.getId())
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void shouldReturn200WithUserDetailsWhenManagerRequests() throws Exception {
        UserResponse response = new UserResponse(manager.getEmail(), manager.getName());

        when(userService.getUserDetails(manager.getId())).thenReturn(response);

        mockMvc.perform(get("/api/users/{id}", manager.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User details retrieved successfully"))
                .andExpect(jsonPath("$.data.email").value(response.getEmail()))
                .andExpect(jsonPath("$.data.name").value(response.getName()));
    }
}
