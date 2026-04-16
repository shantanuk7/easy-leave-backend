package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.HolidayRequest;
import com.technogise.leave_management_system.dto.HolidayResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.HolidayType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.repository.HolidayRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import com.technogise.leave_management_system.service.HolidayService;
import com.technogise.leave_management_system.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration;
import org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(value = HolidayController.class, excludeAutoConfiguration = {
    OAuth2ClientAutoConfiguration.class,
    OAuth2ClientWebSecurityAutoConfiguration.class
})
class HolidayControllerTest {
    @MockitoBean
    private HolidayService holidayService;

    @MockitoBean
    private HolidayRepository holidayRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    private HolidayRequest request;
    private HolidayResponse response;
    private final User manager = new User();

    @BeforeEach
    void setUp() {
        manager.setId(UUID.randomUUID());
        manager.setName("Raj");
        manager.setEmail("raj@technogise.com");
        manager.setRole(UserRole.MANAGER);

        request = new HolidayRequest(
                "Diwali",
                HolidayType.FIXED,
                LocalDate.of(2026, 11, 8)
        );

        response = new HolidayResponse(
                UUID.randomUUID(),
                "Diwali",
                HolidayType.FIXED,
                LocalDate.of(2026, 11, 8));
    }

    private RequestPostProcessor mockUser(User user) {
        return request -> {
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, List.of());
            SecurityContextHolder.getContext().setAuthentication(auth);
            request.setUserPrincipal(auth);
            return request;
        };
    }

    @Test
    void shouldCreateHolidayAndReturn201() throws Exception {
        when(holidayService.createHoliday(any(HolidayRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/holidays")
                        .with(mockUser(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Holiday created successfully"))
                .andExpect(jsonPath("$.data.name").value("Diwali"))
                .andExpect(jsonPath("$.data.type").value("FIXED"))
                .andExpect(jsonPath("$.data.date").value("2026-11-08"));
    }
}
