package com.technogise.leave_management_system.controller;

import com.technogise.leave_management_system.dto.LeaveResponse;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.ForbiddenException;
import com.technogise.leave_management_system.exception.BadRequestException;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import com.technogise.leave_management_system.service.LeaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LeaveController.class)
public class LeaveControllerTest {

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

}
