package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.UpdateUserRoleRequest;
import com.technogise.leave_management_system.dto.EmployeeLeavesRecordResponse;
import com.technogise.leave_management_system.dto.UserResponse;
import com.technogise.leave_management_system.entity.AnnualLeave;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.AnnualLeaveRepository;
import com.technogise.leave_management_system.repository.LeaveCategoryRepository;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private LeaveCategoryRepository leaveCategoryRepository;

    @Mock
    private AnnualLeaveRepository annualLeaveRepository;

    @InjectMocks
    private UserService userService;

    private final String email = "rakshit@technogise.com";
    private final String name = "Rakshit Saxena";
    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        ReflectionTestUtils.setField(userService, "timezone", "Asia/Kolkata");
    }

    @Test
    void shouldReturnExistingUserWhenUserExists() {

        User existingUser = new User();
        existingUser.setEmail(email);
        existingUser.setName(name);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser);

        User result = userService.findOrCreateUser(email, name, "mock-token", Instant.now().plusSeconds(3600));

        assertNotNull(result);
        assertEquals(email, result.getEmail());
    }

    @Test
    void shouldCreateAndReturnNewUserWhenUserDoesNotExist() {

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        User savedUser = new User();
        savedUser.setEmail(email);
        savedUser.setRole(UserRole.EMPLOYEE);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.findOrCreateUser(email, name, "mock-token", Instant.now().plusSeconds(3600));

        assertNotNull(result);
        assertEquals(email, result.getEmail());
        assertEquals(UserRole.EMPLOYEE, result.getRole());
    }

    @Test
    void shouldStoreAccessTokenAndExpiry() {
        Instant expiry = Instant.now().plusSeconds(3600);

        User user = new User();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.findOrCreateUser(email, name, "access-token", expiry);

        assertEquals("access-token", result.getGoogleAccessToken());
        assertNotNull(result.getGoogleTokenExpiry());
    }


    @Test
    void shouldSetDefaultExpiryWhenExpiresAtIsNull() {
        User user = new User();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.findOrCreateUser(
                email, name, "access-token", null);

        assertNotNull(result.getGoogleTokenExpiry());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserIdDoesNotExist() {

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(HttpException.class,
                () -> userService.getUserByUserId(userId));
    }

    @Test
    void shouldReturnUserWhenUserIdDoesExist() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));

        User user = userService.getUserByUserId(userId);

        assertInstanceOf(User.class, user);
    }

    @Test
    void shouldReturnPagedUserResponses() {
        User employee = new User();
        employee.setId(UUID.randomUUID());
        employee.setName("PRIYANSH");
        employee.setEmail("priyansh@technogise.com");
        employee.setRole(UserRole.EMPLOYEE);
        User admin = new User();
        admin.setId(UUID.randomUUID());
        admin.setName("RAJ");
        admin.setEmail("raj@technogise.com");
        admin.setRole(UserRole.ADMIN);

        List<User> users = List.of(employee, admin);
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(users, pageable, users.size());

        when(userRepository.findAllByOrderByNameAsc(any(Pageable.class)))
                .thenReturn(userPage);

        Page<UserResponse> result = userService.getAllUsers(pageable);
        assertEquals(2, result.getContent().size());
        UserResponse first = result.getContent().getFirst();
        assertEquals(employee.getId(), first.getId());
        assertEquals(employee.getEmail(), first.getEmail());
        UserResponse second = result.getContent().get(1);
        assertEquals(admin.getId(), second.getId());
        assertEquals(admin.getEmail(), second.getEmail());
    }

    @Test
    void shouldUpdateRoleSuccessfully() {
        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.EMPLOYEE);

        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest(userId, UserRole.MANAGER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        userService.updateRole(adminId, request);
        assertEquals(UserRole.MANAGER, user.getRole());
    }

    @Test
    void shouldThrowExceptionWhenAdminTriesToUpdateOwnRole() {
        UUID adminId = UUID.randomUUID();
        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest(adminId, UserRole.MANAGER);
        HttpException exception = assertThrows(HttpException.class,
                () -> userService.updateRole(adminId, request));
        assertEquals("You cannot change your own role", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest(userId, UserRole.MANAGER);
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        HttpException exception = assertThrows(HttpException.class,
                () -> userService.updateRole(adminId, request));
        assertEquals("User not found with id: " + userId, exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRoleIsSame() {
        UUID adminId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.MANAGER);
        UpdateUserRoleRequest request =
                new UpdateUserRoleRequest(userId, UserRole.MANAGER);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        HttpException exception = assertThrows(HttpException.class,
                () -> userService.updateRole(adminId, request));
        assertEquals("User already has role: MANAGER", exception.getMessage());
    }

    @Test
    void shouldReturnEmployeeLeaveRecordsWhenLeavesExist() {
        int year = 2026;

        UUID cat1Id = UUID.randomUUID();
        UUID cat2Id = UUID.randomUUID();

        LeaveCategory annual = new LeaveCategory(
                cat1Id, "Annual", 24, LocalDateTime.now(), LocalDateTime.now());
        LeaveCategory paternity = new LeaveCategory(
                cat2Id, "Paternity", 90, LocalDateTime.now(), LocalDateTime.now());

        Leave leave1 = new Leave();
        leave1.setLeaveCategory(annual);
        leave1.setDuration(DurationType.FULL_DAY);

        Leave leave2 = new Leave();
        leave2.setLeaveCategory(annual);
        leave2.setDuration(DurationType.HALF_DAY);

        Leave leave3 = new Leave();
        leave3.setLeaveCategory(paternity);
        leave3.setDuration(DurationType.FULL_DAY);

        List<Leave> allLeaves = new ArrayList<>();
        allLeaves.add(leave1);
        allLeaves.add(leave2);
        for (int i = 0; i < 10; i++) {
            Leave l = new Leave();
            l.setLeaveCategory(paternity);
            l.setDuration(DurationType.FULL_DAY);
            allLeaves.add(l);
        }

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(leaveCategoryRepository.findAll()).thenReturn(List.of(annual, paternity));
        when(leaveRepository.findAllByUserIdAndDateBetweenAndDeletedAtIsNull(
                eq(userId), any(), any(), any(Sort.class)))
                .thenReturn(allLeaves);

        List<EmployeeLeavesRecordResponse> result =
                userService.getEmployeeLeavesRecordByYear(userId, year);

        assertEquals(2, result.size());
        assertEquals(1.5, result.get(0).getLeavesTaken());
        assertEquals(10.0, result.get(1).getLeavesTaken());
    }

    @Test
    void shouldSkipCategoriesWhenNoLeavesTaken() {
        int year = 2026;

        LeaveCategory category = new LeaveCategory(
                UUID.randomUUID(),
                "Annual",
                24,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(new User()));
        when(leaveCategoryRepository.findAll())
                .thenReturn(List.of(category));
        when(leaveRepository.findAllByUserIdAndDateBetweenAndDeletedAtIsNull(
                eq(userId), any(), any(), any(Sort.class)))
                .thenReturn(List.of());

        List<EmployeeLeavesRecordResponse> result =
                userService.getEmployeeLeavesRecordByYear(userId, year);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldUseGivenYearWhenYearIsNotNull() {
        int year = 2026;

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(new User()));

        when(leaveCategoryRepository.findAll())
                .thenReturn(List.of());

        List<EmployeeLeavesRecordResponse> result =
                userService.getEmployeeLeavesRecordByYear(userId, year);

        assertNotNull(result);
    }

    @Test
    void shouldUseCurrentYearWhenYearIsNull() {
        when(userRepository.findById(userId))
                .thenReturn(Optional.of(new User()));

        when(leaveCategoryRepository.findAll())
                .thenReturn(List.of());

        List<EmployeeLeavesRecordResponse> result =
                userService.getEmployeeLeavesRecordByYear(userId, null);

        assertNotNull(result);
    }

    @Test
    void shouldUseAnnualLeaveTotalWhenCategoryIsAnnualAndDataExists() {
        int year = 2026;

        UUID categoryId = UUID.randomUUID();

        LeaveCategory category = new LeaveCategory(
                categoryId,
                "Annual Leave",
                24,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        AnnualLeave annualLeave = new AnnualLeave();
        annualLeave.setTotal(30.0);

        List<Leave> leaves = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Leave leave = new Leave();
            leave.setLeaveCategory(category);
            leave.setDuration(DurationType.FULL_DAY);
            leaves.add(leave);
        }

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(new User()));
        when(leaveCategoryRepository.findAll())
                .thenReturn(List.of(category));
        when(annualLeaveRepository.findByUserIdAndYear(eq(userId), any()))
                .thenReturn(Optional.of(annualLeave));
        when(leaveRepository.findAllByUserIdAndDateBetweenAndDeletedAtIsNull(
                eq(userId), any(), any(), any(Sort.class)))
                .thenReturn(leaves);

        List<EmployeeLeavesRecordResponse> result =
                userService.getEmployeeLeavesRecordByYear(userId, year);

        assertEquals(1, result.size());
        assertEquals(30.0, result.getFirst().getTotalLeavesAvailable());
        assertEquals(5.0, result.getFirst().getLeavesTaken());
        assertEquals(25.0, result.getFirst().getLeavesRemaining());
    }

    @Test
    void shouldReturnUserNameAndEmailForValidUserId() {
        User user = new User();
        user.setId(userId);
        user.setName("Raj");
        user.setEmail("raj@technogise.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse result = userService.getUserDetails(userId);

        assertEquals(result.getEmail(), user.getEmail());
        assertEquals(result.getName(), user.getName());
    }
    @Test
    void shouldCountHalfDayLeaveAs0Point5InLeaveBalance() {
        int year = 2026;
        UUID categoryId = UUID.randomUUID();

        LeaveCategory category = new LeaveCategory(
                categoryId, "Sick Leave", 10,
                LocalDateTime.now(), LocalDateTime.now()
        );

        Leave halfDayLeave = new Leave();
        halfDayLeave.setLeaveCategory(category);
        halfDayLeave.setDuration(DurationType.HALF_DAY);
        halfDayLeave.setDate(LocalDate.of(year, 5, 10));

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(leaveCategoryRepository.findAll()).thenReturn(List.of(category));
        when(leaveRepository.findAllByUserIdAndDateBetweenAndDeletedAtIsNull(
                eq(userId),
                eq(LocalDate.of(year, 1, 1)),
                eq(LocalDate.of(year, 12, 31)),
                any(Sort.class)))
                .thenReturn(List.of(halfDayLeave));

        List<EmployeeLeavesRecordResponse> result =
                userService.getEmployeeLeavesRecordByYear(userId, year);

        assertEquals(1, result.size());
        assertEquals(0.5, result.getFirst().getLeavesTaken());
        assertEquals(9.5, result.getFirst().getLeavesRemaining());
    }


}
