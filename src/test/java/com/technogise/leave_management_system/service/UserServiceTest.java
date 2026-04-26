package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.UpdateUserRoleRequest;
import com.technogise.leave_management_system.dto.EmployeeLeavesRecordResponse;
import com.technogise.leave_management_system.dto.UserResponse;
import com.technogise.leave_management_system.entity.AnnualLeave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
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

import java.time.Instant;
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
    }

    @Test
    void shouldReturnExistingUserWhenUserExists() {

        User existingUser = new User();
        existingUser.setEmail(email);
        existingUser.setName(name);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenReturn(existingUser); // ✅ add this

        User result = userService.findOrCreateUser(email, name, "mock-token", Instant.now().plusSeconds(3600), null);

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

        User result = userService.findOrCreateUser(email, name, "mock-token", Instant.now().plusSeconds(3600), null);

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

        User result = userService.findOrCreateUser(email, name, "access-token", expiry, null);

        assertEquals("access-token", result.getGoogleAccessToken());
        assertNotNull(result.getGoogleTokenExpiry());
    }

    @Test
    void shouldStoreRefreshTokenWhenProvided() {
        User user = new User();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.findOrCreateUser(
                email, name, "access-token", Instant.now(), "refresh-token"
        );

        assertEquals("refresh-token", result.getGoogleRefreshToken());
    }

    @Test
    void shouldNotOverwriteExistingRefreshTokenWhenNullPassed() {
        User user = new User();
        user.setGoogleRefreshToken("existing-refresh-token");

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.findOrCreateUser(
                email, name, "access-token", Instant.now(), null
        );

        assertEquals("existing-refresh-token", result.getGoogleRefreshToken());
    }

    @Test
    void shouldSetDefaultExpiryWhenExpiresAtIsNull() {
        User user = new User();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.findOrCreateUser(
                email, name, "access-token", null, null);

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

        List<LeaveCategory> categories = new ArrayList<>();
        categories.add(new LeaveCategory(
                UUID.randomUUID(),
                "Annual",
                24,
                LocalDateTime.now(),
                LocalDateTime.now())
        );
        categories.add(new LeaveCategory(
                UUID.randomUUID(),
                "Paternity",
                90,
                LocalDateTime.now(),
                LocalDateTime.now())
        );

        when(userRepository.findById(userId)).
                thenReturn(Optional.of(new User()));

        when(leaveCategoryRepository.findAll())
                .thenReturn(categories);

        when(leaveRepository.countByUserIdAndLeaveCategoryIdAndDateBetweenAndDeletedAtIsNull(
                eq(userId), eq(categories.getFirst().getId()), any(), any()))
                .thenReturn(4L);

        when(leaveRepository.countByUserIdAndLeaveCategoryIdAndDateBetweenAndDeletedAtIsNull(
                eq(userId), eq(categories.get(1).getId()), any(), any()))
                .thenReturn(10L);

        List<EmployeeLeavesRecordResponse> result =
                userService.getEmployeeLeavesRecordByYear(userId, year);

        assertEquals(2, result.size());

        EmployeeLeavesRecordResponse annual = result.getFirst();
        assertEquals("Annual", annual.getLeaveType());
        assertEquals(4, annual.getLeavesTaken());
        assertEquals(20, annual.getLeavesRemaining());

        EmployeeLeavesRecordResponse paternity = result.get(1);
        assertEquals("Paternity", paternity.getLeaveType());
        assertEquals(10, paternity.getLeavesTaken());
        assertEquals(80, paternity.getLeavesRemaining());
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

        when(userRepository.findById(userId)).
                thenReturn(Optional.of(new User()));

        when(leaveCategoryRepository.findAll())
                .thenReturn(List.of(category));

        when(leaveRepository.countByUserIdAndLeaveCategoryIdAndDateBetweenAndDeletedAtIsNull(
                any(), any(), any(), any()))
                .thenReturn(0L);

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

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(new User()));

        when(leaveCategoryRepository.findAll())
                .thenReturn(List.of(category));

        when(annualLeaveRepository.findByUserIdAndYear(eq(userId), any()))
                .thenReturn(Optional.of(annualLeave));

        when(leaveRepository.countByUserIdAndLeaveCategoryIdAndDateBetweenAndDeletedAtIsNull(
                any(), any(), any(), any()))
                .thenReturn(5L);

        List<EmployeeLeavesRecordResponse> result =
                userService.getEmployeeLeavesRecordByYear(userId, year);

        assertEquals(1, result.size());
        assertEquals(30.0, result.getFirst().getTotalLeavesAvailable());
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
}
