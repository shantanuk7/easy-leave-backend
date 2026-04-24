package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.CreateRequestPayload;
import com.technogise.leave_management_system.dto.CreateRequestResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.RequestStatus;
import com.technogise.leave_management_system.enums.RequestType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.repository.UserRepository;
import com.technogise.leave_management_system.service.JwtService;
import com.technogise.leave_management_system.service.RequestService;
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
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(value = RequestController.class, excludeAutoConfiguration = {
    OAuth2ClientAutoConfiguration.class,
    OAuth2ClientWebSecurityAutoConfiguration.class
})
class RequestControllerTest {

    @MockitoBean
    private RequestService requestService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private User employee;

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setId(UUID.randomUUID());
        employee.setName("Raj");
        employee.setEmail("raj@technogise.com");
        employee.setRole(UserRole.EMPLOYEE);
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

    private CreateRequestPayload buildValidPayload() {
        CreateRequestPayload payload = new CreateRequestPayload();
        payload.setRequestType(RequestType.PAST_LEAVE);
        payload.setLeaveCategoryId(UUID.randomUUID());
        payload.setDates(List.of(LocalDate.now().minusDays(3)));
        payload.setStartTime(LocalTime.of(9, 0));
        payload.setDuration(DurationType.FULL_DAY);
        payload.setDescription("Was sick but forgot to apply");
        return payload;
    }

    @Test
    void shouldReturn201WithRequestResponseWhenPayloadIsValid() throws Exception {
        CreateRequestPayload payload = buildValidPayload();

        CreateRequestResponse response = new CreateRequestResponse(
                UUID.randomUUID(),
                RequestType.PAST_LEAVE,
                "Sick Leave",
                LocalDate.now().minusDays(3),
                LocalTime.of(9, 0),
                DurationType.FULL_DAY,
                "Was sick but forgot to apply",
                RequestStatus.PENDING
        );

        when(requestService.raiseRequest(any(CreateRequestPayload.class), eq(employee.getId())))
                .thenReturn(List.of(response));

        mockMvc.perform(post("/api/requests")
                        .with(mockUser(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Request(s) raised successfully"))
                .andExpect(jsonPath("$.data[0].requestType").value("PAST_LEAVE"))
                .andExpect(jsonPath("$.data[0].leaveCategoryName").value("Sick Leave"))
                .andExpect(jsonPath("$.data[0].duration").value("FULL_DAY"))
                .andExpect(jsonPath("$.data[0].description").value("Was sick but forgot to apply"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    void shouldReturn400WhenDatesListIsEmpty() throws Exception {
        CreateRequestPayload payload = buildValidPayload();
        payload.setDates(List.of());

        mockMvc.perform(post("/api/requests")
                        .with(mockUser(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest());
    }
}
