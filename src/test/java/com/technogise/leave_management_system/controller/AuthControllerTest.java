package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.AuthUserResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.UserRepository;
import com.technogise.leave_management_system.service.AuthService;
import com.technogise.leave_management_system.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(value = AuthController.class, excludeAutoConfiguration = {
    OAuth2ClientAutoConfiguration.class,
    OAuth2ClientWebSecurityAutoConfiguration.class
})
class AuthControllerTest {
    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtService jwtService;

    @Autowired
    private MockMvc mockMvc;

    private User mockUser;

    @BeforeEach
    void setup() {
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setRole(UserRole.EMPLOYEE);
        mockUser.setEmail("raj@technogise.com");
    }

    private RequestPostProcessor mockUserDetails(User user) {
        return request -> {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
            request.setUserPrincipal(auth);
            return request;
        };
    }

    @Test
    void shouldReturn200WithUserResponseWhenUserIsAuthenticated() throws Exception {
        // Given
        AuthUserResponse response = new AuthUserResponse(
                UUID.randomUUID(),
                "Raj",
                "raj@technogise.com",
                "EMPLOYEE"
        );
        when(authService.getAuthenticatedUser(mockUser.getId())).thenReturn(response);

        // When and Then
        mockMvc.perform(get("/api/auth/me")
                        .with(mockUserDetails(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Raj"))
                .andExpect(jsonPath("$.data.email").value("raj@technogise.com"))
                .andExpect(jsonPath("$.data.role").value("EMPLOYEE"));
    }

    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {
        when(authService.getAuthenticatedUser(mockUser.getId()))
                .thenThrow(new HttpException(HttpStatus.NOT_FOUND, "User not found"));

        mockMvc.perform(get("/api/auth/me")
                        .with(mockUserDetails(mockUser)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }

    @Test
    void shouldReturn200WithSuccessMessageOnLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void shouldClearTokenCookieOnLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().value("token", ""))
                .andExpect(cookie().maxAge("token", 0))
                .andExpect(cookie().httpOnly("token", true))
                .andExpect(cookie().path("token", "/"));
    }
}
