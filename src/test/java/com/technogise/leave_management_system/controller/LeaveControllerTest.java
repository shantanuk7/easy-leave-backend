package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.constants.LeaveConstants;
import com.technogise.leave_management_system.dto.*;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import com.technogise.leave_management_system.service.JwtService;
import com.technogise.leave_management_system.service.LeaveService;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(value = LeaveController.class, excludeAutoConfiguration = {
    OAuth2ClientAutoConfiguration.class,
    OAuth2ClientWebSecurityAutoConfiguration.class
})
public class LeaveControllerTest {

    @MockitoBean
    private LeaveService leaveService;

    @MockitoBean
    private LeaveRepository leaveRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtService jwtService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    User employee = new User();
    User manager = new User();
    LeaveCategory leaveCategory = new LeaveCategory();
    Leave employeeLeave = new Leave();
    Leave managerLeave = new Leave();

    private final UUID categoryId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        employee.setId(UUID.randomUUID());
        employee.setName("Employee");
        employee.setEmail("raj@technogise.com");
        employee.setRole(UserRole.EMPLOYEE);

        manager.setId(UUID.randomUUID());
        manager.setName("Manager");
        manager.setEmail("raj@technogise.com");
        manager.setRole(UserRole.MANAGER);

        leaveCategory.setId(UUID.randomUUID());
        leaveCategory.setName(LeaveConstants.ANNUAL_LEAVE);
        leaveCategory.setCreatedAt(LocalDateTime.now());
        leaveCategory.setUpdatedAt(LocalDateTime.now());

        employeeLeave.setId(UUID.randomUUID());
        employeeLeave.setUser(employee);
        employeeLeave.setLeaveCategory(leaveCategory);
        employeeLeave.setDate(LocalDate.now());
        employeeLeave.setDuration(DurationType.FULL_DAY);
        employeeLeave.setDescription("Personal Work");
        employeeLeave.setStartTime(LocalTime.now());
        employeeLeave.setCreatedAt(LocalDateTime.now());
        employeeLeave.setUpdatedAt(LocalDateTime.now());

        managerLeave.setId(UUID.randomUUID());
        managerLeave.setUser(manager);
        managerLeave.setLeaveCategory(leaveCategory);
        managerLeave.setDate(LocalDate.now());
        managerLeave.setDuration(DurationType.FULL_DAY);
        managerLeave.setDescription("Personal Work");
        managerLeave.setStartTime(LocalTime.now());
        managerLeave.setCreatedAt(LocalDateTime.now());
        managerLeave.setUpdatedAt(LocalDateTime.now());
    }

    private CreateLeaveRequest createValidLeaveRequest() {
        CreateLeaveRequest leaveRequest = new CreateLeaveRequest();
        leaveRequest.setDates(List.of(LocalDate.now()));
        leaveRequest.setLeaveCategoryId(categoryId);
        leaveRequest.setDescription("test leave description");
        leaveRequest.setDuration(DurationType.FULL_DAY);
        leaveRequest.setStartTime(LocalTime.of(12, 0, 0));
        return leaveRequest;
    }

    private CreateLeaveResponse createValidLeaveResponse() {
        CreateLeaveResponse leaveResponse = new CreateLeaveResponse();
        leaveResponse.setType(LeaveConstants.ANNUAL_LEAVE);
        leaveResponse.setDescription("test leave description");
        leaveResponse.setDuration(DurationType.FULL_DAY);
        leaveResponse.setStartTime(LocalTime.of(12, 0, 0));
        leaveResponse.setDate(LocalDate.now());
        return leaveResponse;
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

    private UpdateLeaveRequest createValidUpdateLeaveRequest() {
        UpdateLeaveRequest request = new UpdateLeaveRequest();
        request.setDate(LocalDate.now());
        request.setDuration(DurationType.FULL_DAY);
        request.setLeaveCategoryId(categoryId);
        request.setStartTime(LocalTime.of(10, 0));
        request.setDescription("Updated description");
        return request;
    }

    private UpdateLeaveResponse createValidUpdateLeaveResponse() {
        UpdateLeaveResponse response = new UpdateLeaveResponse();
        response.setId(UUID.randomUUID());
        response.setDate(LocalDate.now().plusDays(3));
        response.setType(LeaveConstants.ANNUAL_LEAVE);
        response.setDuration(DurationType.FULL_DAY);
        response.setStartTime(LocalTime.of(9, 0));
        response.setDescription("Updated description");
        return response;
    }

    @Test
    void shouldReturn200AndListOfLeavesWhenEmployeeRequestsSelfLeaves() throws Exception {
        LeaveResponse response = new LeaveResponse(
                employeeLeave.getId(),
                employeeLeave.getDate(),
                employee.getName(),
                leaveCategory.getName(),
                employeeLeave.getDuration(),
                employeeLeave.getStartTime(),
                employeeLeave.getUpdatedAt(),
                employeeLeave.getDescription(),
                null
        );

        Page<LeaveResponse> page = new PageImpl<>(List.of(response));

        when(leaveService.getAllLeaves(eq(employee.getId()), argThat(filter ->
                        "self".equals(filter.getScope())
                                && filter.getStatus() == null
                                && filter.getEmpId() == null),
                any(Pageable.class))).thenReturn(page);
        when(leaveService.getAllLeaves(eq(employee.getId()), any(LeaveFilterRequest.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/leaves")
                        .with(mockUser(employee))
                        .param("scope", "self"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Leaves retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].duration")
                        .value(response.getDuration().toString()));
    }

    @Test
    void shouldReturn200AndListOfLeavesWhenManagerRequestsSelfLeaves() throws Exception {
        LeaveResponse response = new LeaveResponse(
                managerLeave.getId(),
                managerLeave.getDate(),
                manager.getName(),
                leaveCategory.getName(),
                managerLeave.getDuration(),
                managerLeave.getStartTime(),
                managerLeave.getUpdatedAt(),
                managerLeave.getDescription(),
                null
        );

        Page<LeaveResponse> page = new PageImpl<>(List.of(response));

        when(leaveService.getAllLeaves(eq(manager.getId()), argThat(filter ->
                        "self".equals(filter.getScope())
                                && filter.getStatus() == null
                                && filter.getEmpId() == null),
                any(Pageable.class))).thenReturn(page);
        when(leaveService.getAllLeaves(eq(manager.getId()), any(LeaveFilterRequest.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/leaves")
                        .with(mockUser(manager))
                        .param("scope", "self"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Leaves retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].duration")
                        .value(response.getDuration().toString()));
    }

    @Test
    void shouldApplyAscendingSortWhenSortDirIsAsc() throws Exception {
        Page<LeaveResponse> page = new PageImpl<>(List.of());

        when(leaveService.getAllLeaves(
                eq(employee.getId()),
                any(LeaveFilterRequest.class),
                argThat(pageable ->
                        pageable.getSort().getOrderFor("date") != null
                                && pageable.getSort().getOrderFor("date").isAscending()
                )
        )).thenReturn(page);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/leaves")
                        .with(mockUser(employee))
                        .param("scope", "self")
                        .param("sort", "date")
                        .param("sortDir", "asc"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturn200AndListOfLeavesWhenManagerRequestsAllEmployeeLeaves() throws Exception {
        LeaveResponse response1 = new LeaveResponse(
                managerLeave.getId(),
                managerLeave.getDate(),
                manager.getName(),
                leaveCategory.getName(),
                managerLeave.getDuration(),
                managerLeave.getStartTime(),
                managerLeave.getUpdatedAt(),
                managerLeave.getDescription(),
                null
        );

        LeaveResponse response2 = new LeaveResponse(
                employeeLeave.getId(),
                employeeLeave.getDate(),
                employee.getName(),
                leaveCategory.getName(),
                employeeLeave.getDuration(),
                employeeLeave.getStartTime(),
                employeeLeave.getUpdatedAt(),
                employeeLeave.getDescription(),
                null
        );
        Page<LeaveResponse> page = new PageImpl<>(List.of(response1, response2));

        when(leaveService.getAllLeaves(eq(manager.getId()), argThat(filter -> "self".equals(filter.getScope())
                && filter.getStatus() == null && filter.getEmpId() == null), any(Pageable.class))).thenReturn(page);
        when(leaveService.getAllLeaves(eq(manager.getId()), any(LeaveFilterRequest.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/leaves")
                        .with(mockUser(manager))
                        .param("scope", "organization"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Leaves retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].date").value(response1.getDate().toString()))
                .andExpect(jsonPath("$.data.content[0].employeeName").value(response1.getEmployeeName()))
                .andExpect(jsonPath("$.data.content[0].type").value(response1.getType()))
                .andExpect(jsonPath("$.data.content[0].reason").value(response1.getReason()))
                .andExpect(jsonPath("$.data.content[1].date").value(response2.getDate().toString()))
                .andExpect(jsonPath("$.data.content[1].employeeName").value(response2.getEmployeeName()))
                .andExpect(jsonPath("$.data.content[1].type").value(response2.getType()))
                .andExpect(jsonPath("$.data.content[1].reason").value(response2.getReason()));
    }

    @Test
    void shouldReturn200AndListOfLeavesFilterByStatus() throws Exception {
        LeaveResponse response1 = new LeaveResponse(
                managerLeave.getId(),
                managerLeave.getDate(),
                manager.getName(),
                leaveCategory.getName(),
                managerLeave.getDuration(),
                managerLeave.getStartTime(),
                managerLeave.getUpdatedAt(),
                managerLeave.getDescription(),
                null
                );
        LeaveResponse response2 = new LeaveResponse(
                employeeLeave.getId(),
                employeeLeave.getDate(),
                employee.getName(),
                leaveCategory.getName(),
                employeeLeave.getDuration(),
                employeeLeave.getStartTime(),
                employeeLeave.getUpdatedAt(),
                employeeLeave.getDescription(),
                null
        );

        Page<LeaveResponse> page = new PageImpl<>(List.of(response1, response2));

        when(leaveService.getAllLeaves(eq(manager.getId()), argThat(filter ->
                        "self".equals(filter.getScope())
                                && filter.getStatus() == null
                                && filter.getEmpId() == null),
                any(Pageable.class))).thenReturn(page);
        when(leaveService.getAllLeaves(eq(manager.getId()), any(LeaveFilterRequest.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/leaves")
                        .with(mockUser(manager))
                        .param("scope", "organization")
                        .param("status", "completed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Leaves retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].date")
                        .value(response1.getDate().toString()))
                .andExpect(jsonPath("$.data.content[0].employeeName")
                        .value(response1.getEmployeeName()))
                .andExpect(jsonPath("$.data.content[0].type")
                        .value(response1.getType()))
                .andExpect(jsonPath("$.data.content[0].reason")
                        .value(response1.getReason()));
    }

    @Test
    void shouldReturn400WhenScopeIsNotValid() throws Exception {
        when(leaveService.getAllLeaves(eq(employee.getId()), any(LeaveFilterRequest.class), any(Pageable.class)))
                .thenThrow(new HttpException(HttpStatus.BAD_REQUEST, "Invalid scope query parameter"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/leaves")
                        .with(mockUser(employee))
                        .param("scope", "organization"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("400"))
                .andExpect(jsonPath("$.message").value("Invalid scope query parameter"));
    }

    @Test
    void shouldReturn201WithLeaveResponsesWhenRequestIsValid() throws Exception {
        CreateLeaveRequest request = createValidLeaveRequest();
        CreateLeaveResponse response = createValidLeaveResponse();

        when(leaveService.applyLeave(any(CreateLeaveRequest.class), eq(manager.getId())))
                .thenReturn(List.of(response));

        mockMvc.perform(post("/api/leaves")
                        .with(mockUser(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Leaves applied successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].type").value(LeaveConstants.ANNUAL_LEAVE))
                .andExpect(jsonPath("$.data[0].description").value("test leave description"))
                .andExpect(jsonPath("$.data[0].duration").value("FULL_DAY"));
    }

    @Test
    void shouldReturn200WithEmployeeLeavesWhenManagerProvidesEmpIdAndYear() throws Exception {
        UUID empId = employee.getId();
        int year = 2024;

        LeaveResponse response = new LeaveResponse(
                employeeLeave.getId(),
                employeeLeave.getDate(),
                employee.getName(),
                leaveCategory.getName(),
                employeeLeave.getDuration(),
                employeeLeave.getStartTime(),
                employeeLeave.getUpdatedAt(),
                employeeLeave.getDescription(),
                null
        );

        Page<LeaveResponse> page = new PageImpl<>(List.of(response));

        when(leaveService.getAllLeaves(eq(manager.getId()), argThat(filter ->
                        "self".equals(filter.getScope())
                                && filter.getStatus() == null
                                && filter.getEmpId() == null),
                any(Pageable.class))).thenReturn(page);

        when(leaveService.getAllLeaves(eq(manager.getId()), any(LeaveFilterRequest.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/leaves")
                        .with(mockUser(manager))
                        .param("scope", "ORGANIZATION")
                        .param("empId", empId.toString())
                        .param("year", String.valueOf(year)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Leaves retrieved successfully"))
                .andExpect(jsonPath("$.data.content[0].employeeName").value(employee.getName()))
                .andExpect(jsonPath("$.data.content[0].type").value(leaveCategory.getName()))
                .andExpect(jsonPath("$.data.content[0].duration").value(employeeLeave.getDuration().toString()));
    }

    @Test
    void shouldReturn200WithNoLeavesFoundMessageWhenLeaveListIsEmpty() throws Exception {
        when(leaveService.getAllLeaves(eq(employee.getId()), any(LeaveFilterRequest.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/leaves")
                        .with(mockUser(employee))
                        .param("scope", "self"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("No leave found"))
                .andExpect(jsonPath("$.data.content").isEmpty());
    }

    @Test
    void shouldReturn403WhenEmployeeTryToGetLeaveListWithOrganizationScope() throws Exception {
        when(leaveService.getAllLeaves(eq(employee.getId()), any(LeaveFilterRequest.class), any(Pageable.class)))
                .thenThrow(new HttpException(HttpStatus.FORBIDDEN, "Not Allowed to access this resource"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/leaves")
                        .with(mockUser(employee))
                        .param("scope", "organization"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value("403"))
                .andExpect(jsonPath("$.message").value("Not Allowed to access this resource"));
    }

    // GET /api/leaves/{leaveId} tests
    @Test
    void shouldReturn200WhenLeaveExists() throws Exception {
        LeaveResponse response = new LeaveResponse(
                employeeLeave.getId(),
                employeeLeave.getDate(),
                employee.getName(),
                leaveCategory.getName(),
                employeeLeave.getDuration(),
                employeeLeave.getStartTime(),
                employeeLeave.getUpdatedAt(),
                employeeLeave.getDescription(),
                null
        );

        when(leaveService.getLeaveById(employeeLeave.getId(), employee.getId()))
                .thenReturn(response);

        mockMvc.perform(MockMvcRequestBuilders.get("/api/leaves/{leaveId}", employeeLeave.getId())
                        .with(mockUser(employee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Leave retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(employeeLeave.getId().toString()))
                .andExpect(jsonPath("$.data.employeeName").value(employee.getName()))
                .andExpect(jsonPath("$.data.type").value(leaveCategory.getName()))
                .andExpect(jsonPath("$.data.duration").value(employeeLeave.getDuration().toString()));
    }

    @Test
    void shouldReturn404WhenLeaveNotFound() throws Exception {
        when(leaveService.getLeaveById(employeeLeave.getId(), employee.getId()))
                .thenThrow(new HttpException(HttpStatus.NOT_FOUND, "Leave not found"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/leaves/{leaveId}", employeeLeave.getId())
                        .with(mockUser(employee)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value("404"))
                .andExpect(jsonPath("$.message").value("Leave not found"));
    }

    @Test
    void shouldReturn403WhenUserNotAuthorized() throws Exception {
        when(leaveService.getLeaveById(employeeLeave.getId(), employee.getId()))
                .thenThrow(new HttpException(HttpStatus.FORBIDDEN, "Not Allowed to access this resource"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/leaves/{leaveId}", employeeLeave.getId())
                        .with(mockUser(employee)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value("403"))
                .andExpect(jsonPath("$.message").value("Not Allowed to access this resource"));
    }

    @Test
    void shouldReturn200WithUpdatedLeaveResponseWhenRequestIsValid() throws Exception {
        UUID leaveId = UUID.randomUUID();
        UpdateLeaveRequest request = createValidUpdateLeaveRequest();
        UpdateLeaveResponse response = createValidUpdateLeaveResponse();

        when(leaveService.updateLeave(eq(leaveId), any(UpdateLeaveRequest.class), eq(employee.getId())))
                .thenReturn(response);

        mockMvc.perform(patch("/api/leaves/{id}", leaveId)
                        .with(mockUser(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Leave updated successfully"))
                .andExpect(jsonPath("$.data.date").value(response.getDate().toString()))
                .andExpect(jsonPath("$.data.type").value(LeaveConstants.ANNUAL_LEAVE))
                .andExpect(jsonPath("$.data.duration").value("FULL_DAY"))
                .andExpect(jsonPath("$.data.description").value("Updated description"));
    }

    @Test
    void shouldReturn204WhenLeaveIsCancelledSuccessfully() throws Exception {
        UUID leaveId = UUID.randomUUID();

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/leaves/{id}", leaveId)
                        .with(mockUser(employee)))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldThrow403WhenEmployeeTriesToDeleteLeaveThatBelongsToDifferentUser() throws Exception {
        UUID leaveId = UUID.randomUUID();

        doThrow(new HttpException(HttpStatus.FORBIDDEN, "Not allowed to cancel this leave"))
                .when(leaveService).deleteLeave(leaveId, employee.getId());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/leaves/{id}", leaveId)
                        .with(mockUser(employee)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.statusCode").value("403"))
                .andExpect(jsonPath("$.message").value("Not allowed to cancel this leave"));
    }

    @Test
    void shouldThrow409ConflictErrorWhenEmployeeTriesToDeleteLeaveThatIsAlreadyCancelled() throws Exception {
        UUID leaveId = UUID.randomUUID();

        doThrow(new HttpException(HttpStatus.CONFLICT, "Leave is already cancelled"))
                .when(leaveService).deleteLeave(leaveId, employee.getId());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/leaves/{id}", leaveId)
                        .with(mockUser(employee)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value("409"))
                .andExpect(jsonPath("$.message").value("Leave is already cancelled"));
    }

    @Test
    void shouldReturn400WhenEmployeeTriesToCancelPastLeave() throws Exception {
        UUID leaveId = employeeLeave.getId();

        doThrow(new HttpException(HttpStatus.BAD_REQUEST, "Cannot cancel a past leave"))
                .when(leaveService).deleteLeave(leaveId, employee.getId());

        mockMvc.perform(MockMvcRequestBuilders.delete("/api/leaves/{id}", leaveId)
                        .with(mockUser(employee)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("400"))
                .andExpect(jsonPath("$.message").value("Cannot cancel a past leave"));
    }

    @Test
    void shouldReturn201WhenOptionalHolidayLeaveRequestIsValid() throws Exception {
        CreateLeaveRequest request = new CreateLeaveRequest();
        request.setHolidayId(UUID.randomUUID());
        request.setDates(List.of(LocalDate.now()));
        request.setDuration(DurationType.FULL_DAY);
        request.setStartTime(LocalTime.of(10, 0));
        request.setDescription("Diwali");

        CreateLeaveResponse response = new CreateLeaveResponse();
        response.setType("Diwali");
        response.setDescription("Diwali");
        response.setDuration(DurationType.FULL_DAY);
        response.setDate(LocalDate.now());

        when(leaveService.applyLeave(any(CreateLeaveRequest.class), eq(employee.getId())))
                .thenReturn(List.of(response));

        mockMvc.perform(post("/api/leaves")
                        .with(mockUser(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].type").value("Diwali"))
                .andExpect(jsonPath("$.data[0].duration").value("FULL_DAY"));
    }

    @Test
    void shouldReturn400WhenBothHolidayIdAndLeaveCategoryIdAreProvided() throws Exception {
        CreateLeaveRequest request = new CreateLeaveRequest();
        request.setHolidayId(UUID.randomUUID());
        request.setLeaveCategoryId(UUID.randomUUID());
        request.setDates(List.of(LocalDate.now()));
        request.setDuration(DurationType.FULL_DAY);
        request.setStartTime(LocalTime.of(10, 0));
        request.setDescription("Diwali");

        when(leaveService.applyLeave(any(CreateLeaveRequest.class), eq(employee.getId())))
                .thenThrow(new HttpException(HttpStatus.BAD_REQUEST,
                        "Cannot apply for a leave with both fields provided. Provide either holidayId or leaveCategoryId."));

        mockMvc.perform(post("/api/leaves")
                        .with(mockUser(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("400"))
                .andExpect(jsonPath("$.message").value("Cannot apply for a leave with both fields provided. Provide either holidayId or leaveCategoryId."));
    }

    @Test
    void shouldReturn400WhenOptionalHolidayLimitExceeded() throws Exception {
        CreateLeaveRequest request = new CreateLeaveRequest();
        request.setHolidayId(UUID.randomUUID());
        request.setDates(List.of(LocalDate.now()));
        request.setDuration(DurationType.FULL_DAY);
        request.setStartTime(LocalTime.of(10, 0));
        request.setDescription("Diwali");

        when(leaveService.applyLeave(any(CreateLeaveRequest.class), eq(employee.getId())))
                .thenThrow(new HttpException(HttpStatus.BAD_REQUEST,
                        "Cannot apply more than allocated days for optional holidays"));

        mockMvc.perform(post("/api/leaves")
                        .with(mockUser(employee))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value("400"))
                .andExpect(jsonPath("$.message").value("Cannot apply more than allocated days for optional holidays"));
    }
}
