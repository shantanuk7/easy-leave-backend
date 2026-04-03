package com.technogise.leave_management_system.controller;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(value = UserController.class, excludeAutoConfiguration = {
    OAuth2ClientAutoConfiguration.class,
    OAuth2ClientWebSecurityAutoConfiguration.class
})
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
        employee = new User();
        employee.setId(UUID.randomUUID());
        employee.setName("Priyansh Saxena");
        employee.setRole(UserRole.EMPLOYEE);
        employee.setEmail("priyansh@technogise.com");

        admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setName("ADMIN");
        admin.setRole(UserRole.ADMIN);
        admin.setEmail("admin@technogise.com");
    }

    @Test
    void shouldReturn200WithListOfAllEmployeesWhenManagerOrAdminRequestsDetails() throws Exception {
        List<UserResponse> responses = List.of(
                new UserResponse(employee.getId(), employee.getEmail(), employee.getName(), employee.getRole()),
                new UserResponse(admin.getId(), admin.getEmail(), admin.getName(), admin.getRole())
        );

        when(userService.getAllUsers(admin.getId())).thenReturn(responses);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/users")
                        .with(mockUser(admin)))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Users retrieved successfully"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].id").value(responses.get(0).getId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].name").value(responses.get(0).getName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].id").value(responses.get(1).getId().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].name").value(responses.get(1).getName()));
    }
}
