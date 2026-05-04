package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.UpdateUserRoleRequest;
import com.technogise.leave_management_system.dto.EmployeeLeavesRecordResponse;
import com.technogise.leave_management_system.dto.UserResponse;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.Holiday;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.HolidayType;
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
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    private UUID leaveCategoryId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        leaveCategoryId = UUID.randomUUID();
        ReflectionTestUtils.setField(userService, "timezone", "Asia/Kolkata");
    }


    @Test
    void shouldCoverAllDurationBranchesAndAnnualLeaveLogic() {
        int year = 2026;
        LeaveCategory sickLeave = new LeaveCategory(leaveCategoryId, "Sick Leave", 10, null, null);
        LeaveCategory annualLeaveCategory = new LeaveCategory(UUID.randomUUID(), "Annual Leave", 24, null, null);

        Holiday optionalHoliday = new Holiday();
        optionalHoliday.setId(UUID.randomUUID());
        optionalHoliday.setType(HolidayType.OPTIONAL);

        // Exercise Duration branches for Categories (1.0 vs 0.5)
        Leave catFull = new Leave();
        catFull.setLeaveCategory(sickLeave);
        catFull.setDuration(DurationType.FULL_DAY);

        Leave catHalf = new Leave();
        catHalf.setLeaveCategory(sickLeave);
        catHalf.setDuration(DurationType.HALF_DAY);

        Leave holFull = new Leave();
        holFull.setHoliday(optionalHoliday);
        holFull.setDuration(DurationType.FULL_DAY);

        Leave holHalf = new Leave();
        holHalf.setHoliday(optionalHoliday);
        holHalf.setDuration(DurationType.HALF_DAY);

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(leaveCategoryRepository.findAll()).thenReturn(List.of(sickLeave, annualLeaveCategory));

        when(annualLeaveRepository.findByUserIdAndYear(eq(userId), anyString())).thenReturn(Optional.empty());

        when(leaveRepository.findAllByUserIdAndDateBetweenAndDeletedAtIsNull(eq(userId), any(), any(), any()))
                .thenReturn(List.of(catFull, catHalf, holFull, holHalf));

        List<EmployeeLeavesRecordResponse> result = userService.getEmployeeLeavesRecordByYear(userId, year);

        EmployeeLeavesRecordResponse sickRecord = result.stream()
                .filter(r -> "Sick Leave".equals(r.getLeaveType())).findFirst().get();
        assertEquals(1.5, sickRecord.getLeavesTaken());

        EmployeeLeavesRecordResponse holidayRecord = result.stream()
                .filter(r -> "Optional Holiday".equals(r.getLeaveType())).findFirst().get();
        assertEquals(1.5, holidayRecord.getLeavesTaken());

        EmployeeLeavesRecordResponse annualRecord = result.stream()
                .filter(r -> "Annual Leave".equals(r.getLeaveType())).findFirst().get();
        assertEquals(0.0, annualRecord.getTotalLeavesAvailable());
    }

    @Test
    void shouldReturnExistingUserWhenUserExists() {
        User existingUser = new User();
        existingUser.setEmail(email);
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
        assertEquals(UserRole.EMPLOYEE, result.getRole());
    }

    @Test
    void shouldSetDefaultExpiryWhenExpiresAtIsNull() {
        User user = new User();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        User result = userService.findOrCreateUser(email, name, "access-token", null);
        assertNotNull(result.getGoogleTokenExpiry());
    }

    @Test
    void shouldThrowNotFoundExceptionWhenUserIdDoesNotExist() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(HttpException.class, () -> userService.getUserByUserId(userId));
    }

    @Test
    void shouldReturnPagedUserResponses() {
        User employee = new User();
        employee.setEmail("priyansh@technogise.com");
        Page<User> userPage = new PageImpl<>(List.of(employee));
        when(userRepository.findAllByOrderByNameAsc(any(Pageable.class))).thenReturn(userPage);

        Page<UserResponse> result = userService.getAllUsers(PageRequest.of(0, 10));
        assertEquals(1, result.getContent().size());
    }

    @Test
    void shouldUpdateRoleSuccessfully() {
        User user = new User();
        user.setId(userId);
        user.setRole(UserRole.EMPLOYEE);
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(userId, UserRole.MANAGER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        userService.updateRole(UUID.randomUUID(), request);
        assertEquals(UserRole.MANAGER, user.getRole());
    }

    @Test
    void shouldThrowExceptionWhenAdminTriesToUpdateOwnRole() {
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(userId, UserRole.MANAGER);
        assertThrows(HttpException.class, () -> userService.updateRole(userId, request));
    }

    @Test
    void shouldThrowNotFoundExceptionWhenTargetUserForRoleUpdateDoesNotExist() {
        UUID adminId = UUID.randomUUID();
        UUID nonExistentUserId = UUID.randomUUID();
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(nonExistentUserId, UserRole.MANAGER);

        when(userRepository.findById(nonExistentUserId)).thenReturn(Optional.empty());

        HttpException exception = assertThrows(HttpException.class,
                () -> userService.updateRole(adminId, request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void shouldThrowExceptionWhenAssigningSameRoleToUser() {
        User existingUser = new User();
        existingUser.setId(userId);
        existingUser.setRole(UserRole.MANAGER);
        UpdateUserRoleRequest request = new UpdateUserRoleRequest(userId, UserRole.MANAGER);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existingUser));
        HttpException ex = assertThrows(HttpException.class, () -> userService.updateRole(UUID.randomUUID(), request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }


    @Test
    void shouldReturnUserNameAndEmailForValidUserId() {
        User user = new User();
        user.setName("Raj");
        user.setEmail("raj@technogise.com");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserResponse result = userService.getUserDetails(userId);
        assertEquals("Raj", result.getName());
        assertEquals("raj@technogise.com", result.getEmail());
    }

    @Test
    void shouldDefaultToCurrentYearInKolkataTimezoneWhenYearIsMissing() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User()));
        when(leaveCategoryRepository.findAll()).thenReturn(new ArrayList<>());
        when(leaveRepository.findAllByUserIdAndDateBetweenAndDeletedAtIsNull(eq(userId), any(), any(), any()))
                .thenReturn(new ArrayList<>());

        userService.getEmployeeLeavesRecordByYear(userId, null);

        int currentYear = LocalDate.now(ZoneId.of("Asia/Kolkata")).getYear();
        verify(leaveRepository).findAllByUserIdAndDateBetweenAndDeletedAtIsNull(
                eq(userId), eq(LocalDate.of(currentYear, 1, 1)), eq(LocalDate.of(currentYear, 12, 31)), any());
    }
}
