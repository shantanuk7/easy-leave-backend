package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.LeaveResponse;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.ApplicationException;
import com.technogise.leave_management_system.dto.LeaveRequest;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserService userService;

    @Mock
    private LeaveCategoryService leaveCategoryService;

    @InjectMocks
    private LeaveService leaveService;

    LeaveCategory leaveCategory = new LeaveCategory();

    private User createEmployee() {
        User employee = new User();
        employee.setId(UUID.randomUUID());
        employee.setName("Employee");
        employee.setRole(UserRole.EMPLOYEE);
        return employee;
    }

    private User createManager() {
        User manager = new User();
        manager.setId(UUID.randomUUID());
        manager.setName("Manager");
        manager.setRole(UserRole.MANAGER);
        return manager;
    }

    private LeaveCategory createLeaveCategory() {
        LeaveCategory leaveCategory = new LeaveCategory();
        leaveCategory.setId(UUID.randomUUID());
        leaveCategory.setName("Annual Leave");
        leaveCategory.setCreatedAt(LocalDateTime.now());
        leaveCategory.setUpdatedAt(LocalDateTime.now());
        return leaveCategory;
    }

    private Leave createEmployeeLeave(User employee, LeaveCategory leaveCategory) {
        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(employee);
        leave.setLeaveCategory(leaveCategory);
        leave.setDate(LocalDate.now());
        leave.setDuration(DurationType.FULL_DAY);
        leave.setDescription("Personal Work");
        leave.setStartTime(LocalTime.now());
        leave.setCreatedAt(LocalDateTime.now());
        leave.setUpdatedAt(LocalDateTime.now());
        return leave;
    }

    @Test
    void shouldReturnEmployeeLeavesWhenEmployeeRequestsLeavesWithScopeSelf() {
        User employee = createEmployee();
        Leave employeeLeave = createEmployeeLeave(employee, createLeaveCategory());
        when(leaveRepository.findAllByUserId(employee.getId(), Sort.by("createdAt").descending()))
                .thenReturn(List.of(employeeLeave));

        List<Leave> result = leaveService.filterLeavesByScope("self", employee);
        assertEquals(1, result.size());
        assertEquals(employeeLeave, result.getFirst());
    }
    @Test
    void shouldThrowAccessDeniedWhenEmployeeRequestsLeavesWithScopeOrganization() {
        User employee = createEmployee();
        assertThrows(ApplicationException.class, () ->
                leaveService.filterLeavesByScope("organization", employee)
        );
    }
    @Test
    void shouldReturnAllEmployeeLeavesWhenManagerRequestsLeavesWithScopeOrganization() {
        User employee = createEmployee();
        Leave employeeLeave = createEmployeeLeave(employee, createLeaveCategory());
        User manager = createManager();
        when(leaveRepository.findAll(Sort.by("createdAt").descending()))
                .thenReturn(List.of(employeeLeave));

        List<Leave> result = leaveService.filterLeavesByScope("organization", manager);
        assertEquals(1, result.size());
        assertEquals(employeeLeave, result.getLast());
    }
    @Test
    void shouldThrowBadRequestWhenScopeParamIsInvalid() {
        User employee = createEmployee();
        assertThrows(ApplicationException.class, () ->
                leaveService.filterLeavesByScope("invalid", employee)
        );
    }

    @Test
    void shouldReturnUpcomingLeavesWhenStatusIsUpcoming() {
        Leave futureLeave = new Leave();
        futureLeave.setDate(LocalDate.now().plusDays(1));
        List<Leave> result = leaveService.filterLeavesByStatus("upcoming", List.of(futureLeave));
        assertEquals(1, result.size());
        assertEquals(futureLeave, result.getFirst());
        assertEquals(futureLeave.getLeaveCategory(), result.getFirst().getLeaveCategory());
    }
    @Test
    void shouldReturnOngoingLeavesWhenStatusIsOngoing() {
        Leave leave = new Leave();
        leave.setDate(LocalDate.now());
        User employee = createEmployee();
        Leave employeeLeave = createEmployeeLeave(employee, createLeaveCategory());
        List<Leave> result = leaveService.filterLeavesByStatus("ongoing", List.of(leave,employeeLeave));
        assertEquals(2, result.size());
        assertEquals(leave, result.getFirst());
    }
    @Test
    void shouldReturnPastLeavesWhenStatusIsCompleted() {
        Leave completedLeave = new Leave();
        completedLeave.setDate(LocalDate.now().minusDays(1));
        List<Leave> result = leaveService.filterLeavesByStatus("completed", List.of(completedLeave));
        assertEquals(1, result.size());
        assertEquals(completedLeave, result.getFirst());
        assertEquals(completedLeave.getStartTime(), result.getFirst().getStartTime());
    }
    @Test
    void shouldThrowBadRequestWhenStatusParamIsInvalid() {
        User employee = createEmployee();
        Leave employeeLeave = createEmployeeLeave(employee, createLeaveCategory());
        assertThrows(ApplicationException.class, () ->
                leaveService.filterLeavesByStatus("invalid", List.of(employeeLeave)));
    }
    @Test
    void shouldNotApplyStatusFilterWhenStatusIsNull() {
        User employee = createEmployee();
        Leave employeeLeave = createEmployeeLeave(employee, createLeaveCategory());
        when(userRepository.findById(employee.getId()))
                .thenReturn(Optional.of(employee));

        when(leaveRepository.findAllByUserId(employee.getId(), Sort.by("createdAt").descending()))
                .thenReturn(List.of(employeeLeave));

        List<LeaveResponse> result =
                leaveService.getAllLeaves(employee.getId(), "self", null);
        assertEquals(1, result.size());
        assertEquals("Employee", result.getFirst().employeeName);
    }
    @Test
    void shouldNotApplyStatusFilterWhenStatusIsBlank() {
        User employee = createEmployee();
        Leave employeeLeave = createEmployeeLeave(employee, createLeaveCategory());
        when(userRepository.findById(employee.getId()))
                .thenReturn(Optional.of(employee));

        when(leaveRepository.findAllByUserId(employee.getId(), Sort.by("createdAt").descending()))
                .thenReturn(List.of(employeeLeave));

        List<LeaveResponse> result =
                leaveService.getAllLeaves(employee.getId(), "self", "");
        assertEquals(1, result.size());
        assertEquals("Employee", result.getFirst().employeeName);
    }

    @Test
    void shouldReturnEmployeeUpcomingLeaveWithStatusIsUpcomingAndScopeIsSelf() {
        User employee = createEmployee();
        Leave employeeLeave = createEmployeeLeave(employee, createLeaveCategory());
        employeeLeave.setDate(LocalDate.now().plusDays(1));
        when(userRepository.findById(employee.getId()))
                .thenReturn(Optional.of(employee));
        when(leaveRepository.findAllByUserId(employee.getId(), Sort.by("createdAt").descending()))
                .thenReturn(List.of(employeeLeave));

        List<LeaveResponse> result =
                leaveService.getAllLeaves(employee.getId(), "self", "upcoming");
        assertEquals(1, result.size());
        assertEquals("Employee", result.getFirst().employeeName);
    }
    @Test
    void shouldThrowNotFoundWhenUserDoesNotExist() {
        User employee = createEmployee();
        when(userRepository.findById(employee.getId()))
                .thenReturn(Optional.empty());
        assertThrows(ApplicationException.class, () ->
                leaveService.getAllLeaves(employee.getId(), "self", null)
        );
    }

    private UUID userId;
    private UUID leaveCategoryId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        leaveCategoryId = UUID.randomUUID();
    }

    private LeaveRequest createValidLeaveRequest() {
        LeaveRequest request = new LeaveRequest();
        request.setLeaveCategoryId(leaveCategoryId);
        request.setDates(List.of(LocalDate.now()));
        request.setDuration(DurationType.FULL_DAY);
        request.setStartTime(LocalTime.of(9, 0, 0));
        request.setDescription("Dummy Leave Request description");
        return request;
    }

    private LeaveCategory createValidLeaveCategory() {
        LeaveCategory leaveCategory = new LeaveCategory();
        leaveCategory.setId(leaveCategoryId);
        leaveCategory.setName("Sick Leave");
        return leaveCategory;
    }

    private User createValidUser() {
        User user = new User();
        user.setId(userId);
        return user;
    }

    private Leave createValidLeave() {
        Leave leave = new Leave();
        leave.setLeaveCategory(createValidLeaveCategory());
        leave.setStartTime(LocalTime.of(9, 0, 0));
        leave.setDescription("Dummy Leave description");
        leave.setDuration(DurationType.FULL_DAY);
        return leave;
    }

    @Test
    void shouldAssertWhenLeaveRepositoryMockedSuccessfully() {
        assertInstanceOf(LeaveRepository.class, leaveRepository);
    }

    @Test
    void shouldAssertWhenLeaveServiceMockedSuccessfully() {
        assertInstanceOf(LeaveService.class, leaveService);
    }

    @Test
    void shouldCreateLeaveInstanceWithValidRequest() {
        LeaveRequest request = createValidLeaveRequest();
        LeaveCategory leaveCategory = createValidLeaveCategory();
        User user = createValidUser();

        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId))
                .thenReturn(leaveCategory);
        when(userService.getUserByUserId(userId))
                .thenReturn(user);

        leaveService.applyLeave(request, userId);

        ArgumentCaptor<Leave> leaveCaptor = ArgumentCaptor.forClass(Leave.class);
        verify(leaveRepository).save(leaveCaptor.capture());

        Leave savedLeave = leaveCaptor.getValue();
        assertEquals(request.getDates().getFirst(), savedLeave.getDate());
        assertEquals(leaveCategory, savedLeave.getLeaveCategory());
        assertEquals(user, savedLeave.getUser());
        assertEquals(request.getDescription(), savedLeave.getDescription());
        assertEquals(request.getStartTime(), savedLeave.getStartTime());
        assertEquals(request.getDuration(), savedLeave.getDuration());
    }

    @Test
    void shouldReturnLeaveResponsesWhenLeaveRequestIsValid() {
        LeaveRequest request = createValidLeaveRequest();
        LeaveCategory leaveCategory = createValidLeaveCategory();
        User user = createValidUser();

        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId))
                .thenReturn(leaveCategory);
        when(userService.getUserByUserId(userId))
                .thenReturn(user);

        List<LeaveResponse> leaveResponses = leaveService.applyLeave(request, userId);

        assertInstanceOf(LeaveResponse.class, leaveResponses.getFirst());
    }

    @Test
    void shouldReturnXNumberOfLeaveResponsesWhenXNumberOfDatesProvided() {
        LeaveRequest request = createValidLeaveRequest();
        List<LocalDate> dates = List.of(
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2));
        request.setDates(dates);

        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId))
                .thenReturn(createValidLeaveCategory());
        when(userService.getUserByUserId(userId))
                .thenReturn(createValidUser());

        List<LeaveResponse> leaveResponses = leaveService.applyLeave(request, userId);

        assertEquals(dates.size(), leaveResponses.size());
    }

}
