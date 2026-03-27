package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.LeaveResponse;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.ApplicationException;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

@ExtendWith(MockitoExtension.class)
public class LeaveServiceTest {
    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private UserRepository userRepository;
    @InjectMocks
    private LeaveService leaveService;

    User employee = new User();
    User manager = new User();
    LeaveCategory leaveCategory = new LeaveCategory();
    Leave employeeLeave = new Leave();
    Leave managerLeave = new Leave();

    @BeforeEach
    public void setUp() {
        employee.setId(UUID.randomUUID());
        employee.setName("Employee");
        employee.setRole(UserRole.EMPLOYEE);

        manager.setId(UUID.randomUUID());
        manager.setName("Manager");
        manager.setRole(UserRole.MANAGER);

        leaveCategory.setId(UUID.randomUUID());
        leaveCategory.setName("Annual Leave");
        leaveCategory.setCreatedAt(LocalDateTime.now());
        leaveCategory.setUpdatedAt(LocalDateTime.now());

        employeeLeave.setId(UUID.randomUUID());
        employeeLeave.setUser(employee);
        employeeLeave.setLeaveCategory(leaveCategory);
        employeeLeave.setDate(LocalDate.of(2026, 3, 30));
        employeeLeave.setDuration(DurationType.FULL_DAY);
        employeeLeave.setDescription("Personal Work");
        employeeLeave.setStartTime(LocalTime.now());
        employeeLeave.setCreatedAt(LocalDateTime.now());
        employeeLeave.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    void shouldReturnEmployeeLeavesWhenEmployeeRequestsLeavesWithScopeSelf() {
        when(leaveRepository.findAllByUserId(employee.getId(), Sort.by("createdAt").descending()))
                .thenReturn(List.of(employeeLeave));

        List<Leave> result = leaveService.filterLeavesByScope("self", employee);
        assertEquals(1, result.size());
        assertEquals(employeeLeave, result.getFirst());
    }
    @Test
    void shouldThrowAccessDeniedWhenEmployeeRequestsLeavesWithScopeTeam() {
        assertThrows(ApplicationException.class, () ->
                leaveService.filterLeavesByScope("team", employee)
        );
    }
    @Test
    void shouldReturnAllEmployeeLeavesWhenManagerRequestsLeavesWithScopeTeam() {
        when(leaveRepository.findAll(Sort.by("createdAt").descending()))
                .thenReturn(List.of(managerLeave, employeeLeave));

        List<Leave> result = leaveService.filterLeavesByScope("team", manager);
        assertEquals(2, result.size());
        assertEquals(managerLeave, result.getFirst());
        assertEquals(employeeLeave, result.getLast());
    }
    @Test
    void shouldThrowBadRequestWhenScopeParamIsInvalid() {
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
        List<Leave> result = leaveService.filterLeavesByStatus("ongoing", List.of(leave,employeeLeave));
        assertEquals(1, result.size());
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
        assertThrows(ApplicationException.class, () ->
                leaveService.filterLeavesByStatus("invalid", List.of(employeeLeave)));
    }
    @Test
    void shouldNotApplyStatusFilterWhenStatusIsNull() {
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
        when(userRepository.findById(employee.getId()))
                .thenReturn(Optional.empty());
        assertThrows(ApplicationException.class, () ->
                leaveService.getAllLeaves(employee.getId(), "self", null)
        );
    }
}

