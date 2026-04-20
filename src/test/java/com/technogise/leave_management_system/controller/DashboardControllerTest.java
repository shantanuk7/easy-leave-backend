package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.EmployeeMetricsResponse;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import com.technogise.leave_management_system.service.DashboardService;
import com.technogise.leave_management_system.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@WebMvcTest(value = DashboardController.class, excludeAutoConfiguration = {
    OAuth2ClientAutoConfiguration.class,
    OAuth2ClientWebSecurityAutoConfiguration.class
})
@EnableMethodSecurity(prePostEnabled = true)
public class DashboardControllerTest {
    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private LeaveRepository leaveRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "MANAGER")
    void shouldReturn200WithListOfAllDetailsForManagerDashboard() throws Exception {
        EmployeeMetricsResponse response = new EmployeeMetricsResponse(20,20,20);
        when(dashboardService.getManagerDashboardData()).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/dashboard/manager"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Manager Dashboard data retrieved successfully"))
                .andExpect(jsonPath("$.data.totalEmployees").value(20));
    }
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void shouldReturn403WhenEmployeeRequestsManagerDashboard() throws Exception {
        EmployeeMetricsResponse response = new EmployeeMetricsResponse(20,20,20);
        when(dashboardService.getManagerDashboardData()).thenReturn(response);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/dashboard/manager"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access Denied"));
    }
}


