package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.EmployeeMetricsResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import com.technogise.leave_management_system.service.DashboardService;
import com.technogise.leave_management_system.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(value = DashboardController.class, excludeAutoConfiguration = {
    OAuth2ClientAutoConfiguration.class,
    OAuth2ClientWebSecurityAutoConfiguration.class
})
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

    private User manager;

    private RequestPostProcessor mockUser(User user) {
        return request -> {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
            request.setUserPrincipal(auth);
            return request;
        };
    }

    @BeforeEach
    void setUp() {
        manager = new User();
        manager.setId(UUID.randomUUID());
        manager.setName("Priyansh Saxena");
        manager.setRole(UserRole.EMPLOYEE);
        manager.setEmail("priyansh@technogise.com");
    }

    @Test
    void shouldReturn200WithListOfAllDetailsForManagerDashboard() throws Exception {
        EmployeeMetricsResponse response = new EmployeeMetricsResponse(20,20,20);
        when(dashboardService.getManagerDashboardData()).thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/dashboard/manager")
                        .with(mockUser(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Manager Dashboard data retrieved successfully"))
                .andExpect(jsonPath("$.data.totalEmployees").value(20));
    }
}


