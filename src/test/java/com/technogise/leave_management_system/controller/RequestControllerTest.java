package com.technogise.leave_management_system.controller;
import com.technogise.leave_management_system.dto.RequestResponse;

import com.technogise.leave_management_system.dto.CreateRequestPayload;
import com.technogise.leave_management_system.dto.CreateRequestResponse;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.RequestStatus;
import com.technogise.leave_management_system.enums.ScopeType;
import com.technogise.leave_management_system.enums.RequestType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.ObjectMapper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
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

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private User employee;


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
        employee.setEmail("priyansh@technogise.com");
        employee.setRole(UserRole.EMPLOYEE);
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
    void shouldReturn200AndListOfRequestsForSelfScope() throws Exception {
        List<RequestResponse> mockResponse = List.of( new RequestResponse(
                UUID.randomUUID(),
                employee.getName(),
                null,
                "Sick Leave",
                LocalDate.now(),
                DurationType.FULL_DAY,
                "Fever",
                RequestStatus.PENDING,
                LocalDate.now()
        ));
        Page<RequestResponse> mockPage = new PageImpl<>(mockResponse);
        when(requestService.getAllRequests( any(Pageable.class), eq(employee.getId()), eq(ScopeType.SELF), eq(null))).thenReturn(mockPage);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/requests")
                        .param("scope", "SELF")
                        .param("page", "0")
                        .param("size", "20")
                        .with(mockUser(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Requests fetch Successfully"))
                .andExpect(jsonPath("$.data.content[0].employeeName").value("Priyansh Saxena"))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"));
    }
    @Test
    void shouldReturn403WhenEmployeeAccessesOrganizationScope() throws Exception {
        when(requestService.getAllRequests(
                any(Pageable.class),
                eq(employee.getId()),
                eq(ScopeType.ORGANIZATION),
                any()
        )).thenThrow(new HttpException(HttpStatus.FORBIDDEN, "Not allowed to access this resource"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/requests")
                        .param("scope", "ORGANIZATION")
                        .with(mockUser(employee)))
                .andExpect(status().isForbidden());
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
    @Test
    void shouldReturn201WithCompOffResponseWhenPayloadIsValid() throws Exception {
        LocalDate lastSaturday = LocalDate.now()
                .minusDays(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));

        CreateRequestPayload payload = new CreateRequestPayload();
        payload.setRequestType(RequestType.COMPENSATORY_OFF);
        payload.setDates(List.of(lastSaturday));
        payload.setStartTime(LocalTime.of(10, 0));
        payload.setDuration(DurationType.FULL_DAY);
        payload.setDescription("Worked on Saturday for release");

        CreateRequestResponse response = new CreateRequestResponse(
                UUID.randomUUID(),
                RequestType.COMPENSATORY_OFF,
                null,
                lastSaturday,
                LocalTime.of(10, 0),
                DurationType.FULL_DAY,
                "Worked on Saturday for release",
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
                .andExpect(jsonPath("$.data[0].requestType").value("COMPENSATORY_OFF"))
                .andExpect(jsonPath("$.data[0].leaveCategoryName").doesNotExist())
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }
}
