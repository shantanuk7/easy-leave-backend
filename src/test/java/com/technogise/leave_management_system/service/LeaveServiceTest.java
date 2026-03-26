package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.LeaveResponse;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.ForbiddenException;
import com.technogise.leave_management_system.exception.BadRequestException;
import com.technogise.leave_management_system.exception.NotFoundException;
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
        assertThrows(ForbiddenException.class, () ->
                leaveService.filterLeavesByScope("team", employee)
        );
    }
    @Test
    void shouldReturnAllEmployeeLeavesWhenManagerRequestsLeavesWithScopeTeam() {
        when(leaveRepository.findAll(Sort.by("createdAt").descending()))
                .thenReturn(List.of(managerLeave, employeeLeave));

        List<Leave> result = leaveService.filterLeavesByScope("team", manager);
        assertEquals(2, result.size());
        assertEquals(employeeLeave, result.getFirst());
        assertEquals(employeeLeave, result.getLast());
    }
    @Test
    void shouldThrowBadRequestWhenScopeParamIsInvalid() {
        assertThrows(BadRequestException.class, () ->
                leaveService.filterLeavesByScope("invalid", employee)
        );
    }
}

