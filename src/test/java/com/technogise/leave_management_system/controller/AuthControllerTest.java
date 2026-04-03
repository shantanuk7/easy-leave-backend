package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.AuthUserResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AuthController.class)
class AuthControllerTest {
    @MockitoBean
    private AuthService authService;

    @Autowired
    private MockMvc mockMvc;

    private User mockUser;

    @BeforeEach
    void setup() {
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setRole(UserRole.EMPLOYEE);
        mockUser.setEmail("raj@gmail.com");
    }

    @Test
    void shouldReturn200WithUserResponseWhenUserIsAuthenticated() throws Exception {
        // Given
        AuthUserResponse response = new AuthUserResponse(
                UUID.randomUUID(),
                "Raj",
                "raj@gmail.com",
                "EMPLOYEE"
        );
        when(authService.getAuthenticatedUser(mockUser.getId())).thenReturn(response);

        // When and Then
        mockMvc.perform(get("/api/auth/me")
                        .header("user_id", mockUser.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Raj"))
                .andExpect(jsonPath("$.data.email").value("raj@gmail.com"))
                .andExpect(jsonPath("$.data.role").value("EMPLOYEE"));
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        when(authService.getAuthenticatedUser(mockUser.getId()))
                .thenThrow(new HttpException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(get("/api/auth/me")
                        .header("user_id", mockUser.getId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}
