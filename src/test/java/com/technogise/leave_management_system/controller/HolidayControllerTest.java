package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.HolidayRequest;
import com.technogise.leave_management_system.dto.HolidayResponse;
import com.technogise.leave_management_system.enums.HolidayType;
import com.technogise.leave_management_system.exception.HttpException;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@AutoConfigureMockMvc
@WebMvcTest(value = HolidayController.class, excludeAutoConfiguration = {
    OAuth2ClientAutoConfiguration.class,
    OAuth2ClientWebSecurityAutoConfiguration.class
})
@EnableMethodSecurity
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

    @BeforeEach
    void setUp() {
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

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldCreateHolidayAndReturn201() throws Exception {
        when(holidayService.createHoliday(any(HolidayRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/holidays")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Holiday created successfully"))
                .andExpect(jsonPath("$.data.name").value("Diwali"))
                .andExpect(jsonPath("$.data.type").value("FIXED"))
                .andExpect(jsonPath("$.data.date").value("2026-11-08"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void shouldReturn403WhenNonAdminUserWantsToCreateHoliday() throws Exception {
        when(holidayService.createHoliday(any(HolidayRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/holidays")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access Denied"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturn200WithListOfAllHolidays() throws Exception {
        when(holidayService.getHolidays(null)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/holidays")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Holidays retrieved successfully"))
                .andExpect(jsonPath("$.data[0].name").value("Diwali"))
                .andExpect(jsonPath("$.data[0].type").value("FIXED"))
                .andExpect(jsonPath("$.data[0].date").value("2026-11-08"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldThrow400BadRequestWhenHolidayTypeIsInvalid() throws Exception {
        when(holidayService.getHolidays("RANDOM")).thenThrow(new HttpException(HttpStatus.BAD_REQUEST, "Invalid holiday type parameter"));

        mockMvc.perform(get("/api/holidays?type=RANDOM"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid holiday type parameter"));
    }
}
