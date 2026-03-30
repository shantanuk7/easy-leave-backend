package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.LeaveResponse;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.dto.CreateLeaveRequest;
import com.technogise.leave_management_system.dto.CreateLeaveResponse;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.ApplicationException;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import com.technogise.leave_management_system.service.LeaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveController.class)
public class LeaveControllerTest {
class LeaveControllerTest {

    @MockitoBean
    private LeaveService leaveService;

    @MockitoBean
    private LeaveRepository leaveRepository;

    @MockitoBean
    private UserRepository userRepository;

    @Autowired
    private MockMvc mockMvc;

    User employee = new User();
    User manager = new User();
    LeaveCategory leaveCategory = new LeaveCategory();
    Leave employeeLeave = new Leave();
    Leave managerLeave = new Leave();

    @BeforeEach
    void setup() {
        employee.setId(UUID.randomUUID());
        employee.setName("Employee");
        employee.setRole(UserRole.EMPLOYEE);
    @Autowired
    private ObjectMapper objectMapper;

        manager.setId(UUID.randomUUID());
        manager.setName("Manager");
        manager.setRole(UserRole.MANAGER);
    private final UUID userId     = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();

        leaveCategory.setId(UUID.randomUUID());
        leaveCategory.setName("Annual Leave");
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
    private CreateLeaveRequest createValidLeaveRequest() {
        CreateLeaveRequest leaveRequest = new CreateLeaveRequest();
        leaveRequest.setDates(List.of(LocalDate.now()));
        leaveRequest.setLeaveCategoryId(categoryId);
        leaveRequest.setDescription("test leave description");
        leaveRequest.setDuration(DurationType.FULL_DAY);
        leaveRequest.setStartTime(LocalTime.of(12, 0, 0));
        return leaveRequest;
    }

        managerLeave.setId(UUID.randomUUID());
        managerLeave.setUser(manager);
        managerLeave.setLeaveCategory(leaveCategory);
        managerLeave.setDate(LocalDate.now());
        managerLeave.setDuration(DurationType.FULL_DAY);
        managerLeave.setDescription("Personal Work");
        managerLeave.setStartTime(LocalTime.now());
        managerLeave.setCreatedAt(LocalDateTime.now());
        managerLeave.setUpdatedAt(LocalDateTime.now());
    private CreateLeaveResponse createValidLeaveResponse() {
        CreateLeaveResponse leaveResponse = new CreateLeaveResponse();
        leaveResponse.setLeaveCategoryName("Annual Leave");
        leaveResponse.setDescription("test leave description");
        leaveResponse.setDuration(DurationType.FULL_DAY);
        leaveResponse.setStartTime(LocalTime.of(12, 0, 0));
        leaveResponse.setDate(LocalDate.now());
        return leaveResponse;
    }

    @Test
    void shouldReturn200AndListOfLeavesWhenEmployeeRequestsSelfLeaves() throws Exception {
        List<LeaveResponse> response = List.of(
                new LeaveResponse(
                        employeeLeave.getId(),
                        employeeLeave.getDate(),
                        employee.getName(),
                        leaveCategory.getName(),
                        employeeLeave.getDuration(),
                        employeeLeave.getStartTime(),
                        employeeLeave.getUpdatedAt(),
                        employeeLeave.getDescription()
                )
        );

        when(leaveService.getAllLeaves(employee.getId(), "self", null))
                .thenReturn(response);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/leaves")
                        .header("user_id", employee.getId())
                        .param("scope", "self"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Leaves retrieved successfully"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].duration")
                        .value(response.getFirst().getDuration().toString()));
    }
    @Test
    void shouldReturn200AndListOfLeavesWhenManagerRequestsSelfLeaves() throws Exception {
        List<LeaveResponse> response = List.of(
                new LeaveResponse(
                        managerLeave.getId(),
                        managerLeave.getDate(),
                        manager.getName(),
                        leaveCategory.getName(),
                        managerLeave.getDuration(),
                        managerLeave.getStartTime(),
                        managerLeave.getUpdatedAt(),
                        managerLeave.getDescription()
                )
        );
        when(leaveService.getAllLeaves(manager.getId(), "self", null))
                .thenReturn(response);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/leaves")
                        .header("user_id", manager.getId())
                        .param("scope", "self"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Leaves retrieved successfully"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].duration")
                        .value(response.getFirst().getDuration().toString()));
    }

    @Test
    void shouldReturn200AndListOfLeavesWhenManagerRequestsAllEmployeeLeaves() throws Exception {
        List<LeaveResponse> response = List.of(
                new LeaveResponse(
                        managerLeave.getId(),
                        managerLeave.getDate(),
                        manager.getName(),
                        leaveCategory.getName(),
                        managerLeave.getDuration(),
                        managerLeave.getStartTime(),
                        managerLeave.getUpdatedAt(),
                        managerLeave.getDescription()
                ),
                new LeaveResponse(
                        employeeLeave.getId(),
                        employeeLeave.getDate(),
                        employee.getName(),
                        leaveCategory.getName(),
                        employeeLeave.getDuration(),
                        employeeLeave.getStartTime(),
                        employeeLeave.getUpdatedAt(),
                        employeeLeave.getDescription()
                )
        );
        when(leaveService.getAllLeaves(manager.getId(),"organization", null))
                .thenReturn(response);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/leaves")
                        .header("user_id", manager.getId())
                        .param("scope", "organization"))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Leaves retrieved successfully"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].date")
                        .value(response.getFirst().getDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].employeeName")
                        .value(response.getFirst().getEmployeeName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].type")
                        .value(response.getFirst().getType()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].reason")
                        .value(response.getFirst().getReason()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].date")
                    .value(response.get(1).getDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].employeeName")
                        .value(response.get(1).getEmployeeName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].type")
                        .value(response.get(1).getType()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[1].reason")
                        .value(response.get(1).getReason()));
    }
    @Test
    void shouldReturn200AndListOfLeavesFilterByStatus() throws Exception {
        List<LeaveResponse> response = List.of(
                new LeaveResponse(
                        managerLeave.getId(),
                        managerLeave.getDate(),
                        manager.getName(),
                        leaveCategory.getName(),
                        managerLeave.getDuration(),
                        managerLeave.getStartTime(),
                        managerLeave.getUpdatedAt(),
                        managerLeave.getDescription()
                ),
                new LeaveResponse(
                        employeeLeave.getId(),
                        employeeLeave.getDate(),
                        employee.getName(),
                        leaveCategory.getName(),
                        employeeLeave.getDuration(),
                        employeeLeave.getStartTime(),
                        employeeLeave.getUpdatedAt(),
                        employeeLeave.getDescription()
                )
        );
        when(leaveService.getAllLeaves(manager.getId(),"organization","completed" ))
                .thenReturn(response);
        mockMvc.perform(MockMvcRequestBuilders.get("/api/leaves")
                        .header("user_id", manager.getId())
                        .param("scope", "organization")
                        .param("status", "completed")
                )
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.success").value(true))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Leaves retrieved successfully"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].date")
                        .value(response.getFirst().getDate().toString()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].employeeName")
                        .value(response.getFirst().getEmployeeName()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].type")
                        .value(response.getFirst().getType()))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data[0].reason")
                        .value(response.getFirst().getReason()));
    }
    @Test
    void shouldReturn400WhenScopeIsNotValid() throws Exception {
        when(leaveService.getAllLeaves(employee.getId(),"organization",null))
                .thenThrow(new ApplicationException(HttpStatus.BAD_REQUEST,"Invalid scope query parameter"));
    void shouldReturn201WithLeaveResponsesWhenRequestIsValid() throws Exception {
        CreateLeaveRequest  request  = createValidLeaveRequest();
        CreateLeaveResponse response = createValidLeaveResponse();

        mockMvc.perform(MockMvcRequestBuilders.get("/api/leaves")
                        .header("user_id", employee.getId())
                        .param("scope", "organization")
                )
                .andExpect(status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.statusCode").value("400"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Invalid scope query parameter"));
        when(leaveService.applyLeave(any(CreateLeaveRequest.class), eq(userId)))
                .thenReturn(List.of(response));

        mockMvc.perform(post("/api/leaves")
                        .header("user_id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Leaves applied successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].leaveCategoryName").value("Annual Leave"))
                .andExpect(jsonPath("$.data[0].description").value("test leave description"))
                .andExpect(jsonPath("$.data[0].duration").value("FULL_DAY"));
    }
    @Test
    void shouldReturn403WhenEmployeeTryToGetLeaveListWithOrganizationScope() throws Exception {
        when(leaveService.getAllLeaves(employee.getId(),"organization",null))
                .thenThrow(new ApplicationException(HttpStatus.FORBIDDEN,"Not Allowed to access this resource"));

        mockMvc.perform(MockMvcRequestBuilders.get("/api/leaves")
                        .header("user_id", employee.getId())
                        .param("scope", "organization")
                )
                .andExpect(status().isForbidden())
                .andExpect(MockMvcResultMatchers.jsonPath("$.statusCode").value("403"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("Not Allowed to access this resource"));
    }
}
