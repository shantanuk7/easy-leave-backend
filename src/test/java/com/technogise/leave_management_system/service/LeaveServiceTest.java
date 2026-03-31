package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.CreateLeaveRequest;
import com.technogise.leave_management_system.dto.CreateLeaveResponse;
import com.technogise.leave_management_system.dto.LeaveResponse;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.LeaveRepository;
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
import java.util.Arrays;
import java.util.List;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private UserService userService;

    @Mock
    private LeaveCategoryService leaveCategoryService;

    @InjectMocks
    private LeaveService leaveService;

    private UUID userId;
    private UUID leaveCategoryId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        leaveCategoryId = UUID.randomUUID();
    }


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

    private CreateLeaveRequest createValidLeaveRequest() {
        CreateLeaveRequest request = new CreateLeaveRequest();
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
    @Test
    void shouldReturnEmployeeLeavesWhenEmployeeRequestsLeavesWithScopeSelf() {
        User employee = createEmployee();
        Leave employeeLeave = createEmployeeLeave(employee, createLeaveCategory());
        when(leaveRepository.findAllByUserId(employee.getId(), Sort.by(Sort.Direction.DESC, "date")))
                .thenReturn(List.of(employeeLeave));

        List<Leave> result = leaveService.filterLeavesByScope("self", employee);

        assertEquals(1, result.size());
        assertEquals(employeeLeave, result.getFirst());
    }

    @Test
    void shouldThrowAccessDeniedWhenEmployeeRequestsLeavesWithScopeOrganization() {
        User employee = createEmployee();

        assertThrows(HttpException.class, () ->
                leaveService.filterLeavesByScope("organization", employee));
    }

    @Test
    void shouldReturnAllEmployeeLeavesWhenManagerRequestsLeavesWithScopeOrganization() {
        User employee = createEmployee();
        Leave employeeLeave = createEmployeeLeave(employee, createLeaveCategory());
        User manager = createManager();
        when(leaveRepository.findAll(Sort.by(Sort.Direction.DESC, "date")))
                .thenReturn(List.of(employeeLeave));

        List<Leave> result = leaveService.filterLeavesByScope("organization", manager);

        assertEquals(1, result.size());
        assertEquals(employeeLeave, result.getLast());
    }

    @Test
    void shouldThrowBadRequestWhenScopeParamIsInvalid() {
        User employee = createEmployee();

        assertThrows(HttpException.class, () ->
                leaveService.filterLeavesByScope("invalid", employee));
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
        User employee = createEmployee();
        Leave todayLeave = new Leave();
        todayLeave.setDate(LocalDate.now());
        Leave employeeLeave = createEmployeeLeave(employee, createLeaveCategory());

        List<Leave> result = leaveService.filterLeavesByStatus("ongoing", List.of(todayLeave, employeeLeave));

        assertEquals(2, result.size());
        assertEquals(todayLeave, result.getFirst());
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

        assertThrows(HttpException.class, () ->
                leaveService.filterLeavesByStatus("invalid", List.of(employeeLeave)));
    }

    @Test
    void shouldNotApplyStatusFilterWhenStatusIsNull() {
        User employee = createEmployee();
        Leave employeeLeave = createEmployeeLeave(employee, createLeaveCategory());
        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(leaveRepository.findAllByUserId(employee.getId(), Sort.by(Sort.Direction.DESC, "date")))
                .thenReturn(List.of(employeeLeave));

        List<LeaveResponse> result = leaveService.getAllLeaves(employee.getId(), "self", null);

        assertEquals(1, result.size());
        assertEquals("Employee", result.getFirst().employeeName);
    }

    @Test
    void shouldNotApplyStatusFilterWhenStatusIsBlank() {
        User employee = createEmployee();
        Leave employeeLeave = createEmployeeLeave(employee, createLeaveCategory());
        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(leaveRepository.findAllByUserId(employee.getId(), Sort.by(Sort.Direction.DESC, "date")))
                .thenReturn(List.of(employeeLeave));

        List<LeaveResponse> result = leaveService.getAllLeaves(employee.getId(), "self", "");

        assertEquals(1, result.size());
        assertEquals("Employee", result.getFirst().employeeName);
    }

    @Test
    void shouldReturnEmployeeUpcomingLeaveWhenStatusIsUpcomingAndScopeIsSelf() {
        User employee = createEmployee();
        Leave employeeLeave = createEmployeeLeave(employee, createLeaveCategory());
        employeeLeave.setDate(LocalDate.now().plusDays(1));
        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(leaveRepository.findAllByUserId(employee.getId(), Sort.by(Sort.Direction.DESC, "date")))
                .thenReturn(List.of(employeeLeave));

        List<LeaveResponse> result = leaveService.getAllLeaves(employee.getId(), "self", "upcoming");

        assertEquals(1, result.size());
        assertEquals("Employee", result.getFirst().employeeName);
    }

    @Test
    void shouldThrowNotFoundWhenUserDoesNotExist() {
        User employee = createEmployee();
        when(userService.getUserByUserId(employee.getId()))
                .thenThrow(new HttpException(org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));

        assertThrows(HttpException.class, () ->
                leaveService.getAllLeaves(employee.getId(), "self", null));
    }

    @Test
    void shouldReturnTrueForToday() {
        assertTrue(leaveService.isValidLeaveDate(LocalDate.now()));
    }

    @Test
    void shouldReturnTrueForFutureDateInCurrentYear() {
        assertTrue(leaveService.isValidLeaveDate(LocalDate.now().plusDays(10)));
    }

    @Test
    void shouldReturnTrueForPastDateInCurrentMonth() {
        if (LocalDate.now().getDayOfMonth() > 1) {
            LocalDate pastDateThisMonth = LocalDate.now().withDayOfMonth(1);
            assertTrue(leaveService.isValidLeaveDate(pastDateThisMonth));
        }
    }

    @Test
    void shouldReturnFalseForPastDateInPreviousMonth() {
        LocalDate lastMonth = LocalDate.now().minusMonths(1);
        assertFalse(leaveService.isValidLeaveDate(lastMonth));
    }

    @Test
    void shouldReturnFalseForDateInNextYear() {
        assertFalse(leaveService.isValidLeaveDate(LocalDate.now().plusYears(1)));
    }

    @Test
    void shouldReturnFalseForDateInPreviousYear() {
        assertFalse(leaveService.isValidLeaveDate(LocalDate.now().minusYears(1)));
    }

    @Test
    void shouldReturnTrueForSaturday() {
        assertTrue(leaveService.isWeekendDay(LocalDate.of(2026, 4, 4)));
    }

    @Test
    void shouldReturnTrueForSunday() {
        assertTrue(leaveService.isWeekendDay(LocalDate.of(2026, 4, 5)));
    }

    @Test
    void shouldReturnFalseForMonday() {
        assertFalse(leaveService.isWeekendDay(LocalDate.of(2026, 4, 6)));
    }

    @Test
    void shouldReturnFalseForTuesday() {
        assertFalse(leaveService.isWeekendDay(LocalDate.of(2026, 4, 7)));
    }

    @Test
    void shouldSaveLeaveWithCorrectFieldsWhenRequestIsValid() {
        CreateLeaveRequest request = createValidLeaveRequest();
        LeaveCategory leaveCategory = createValidLeaveCategory();
        User user = createValidUser();

        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(leaveCategory);
        when(userService.getUserByUserId(userId)).thenReturn(user);
        when(leaveRepository.findAllByUserId(eq(userId), any(Sort.class))).thenReturn(List.of());

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
    void shouldReturnCreateLeaveResponseWhenRequestIsValid() {
        CreateLeaveRequest request = createValidLeaveRequest();
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(createValidLeaveCategory());
        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveRepository.findAllByUserId(eq(userId), any(Sort.class))).thenReturn(List.of());

        List<CreateLeaveResponse> responses = leaveService.applyLeave(request, userId);

        assertInstanceOf(CreateLeaveResponse.class, responses.getFirst());
    }

    @Test
    void shouldReturnOneResponsePerNewDateProvided() {
        CreateLeaveRequest request = createValidLeaveRequest();
        List<LocalDate> dates = List.of(
                LocalDate.now(),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2)
        );
        request.setDates(dates);

        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(createValidLeaveCategory());
        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveRepository.findAllByUserId(eq(userId), any(Sort.class))).thenReturn(List.of());

        List<CreateLeaveResponse> responses = leaveService.applyLeave(request, userId);

        assertEquals(dates.size(), responses.size());
    }

    @Test
    void shouldSkipExistingDatesAndSaveOnlyNewOnesWhenOverlappingDatesExist() {
        User user = createValidUser();
        LeaveCategory category = createValidLeaveCategory();

        LocalDate dayOne = LocalDate.of(2026, 3, 2);
        LocalDate dayTwo = LocalDate.of(2026, 3, 3); // already exists
        LocalDate dayThree = LocalDate.of(2026, 3, 4);

        Leave existingLeaveOnDayTwo = new Leave();
        existingLeaveOnDayTwo.setDate(dayTwo);
        existingLeaveOnDayTwo.setUser(user);

        CreateLeaveRequest request = new CreateLeaveRequest(
                leaveCategoryId,
                Arrays.asList(dayOne, dayTwo, dayThree),
                DurationType.FULL_DAY,
                LocalTime.of(9, 0),
                "Dummy Leave Description"
        );

        when(userService.getUserByUserId(userId)).thenReturn(user);
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(category);
        when(leaveRepository.findAllByUserId(eq(userId), any(Sort.class)))
                .thenReturn(List.of(existingLeaveOnDayTwo));

        List<CreateLeaveResponse> responses = leaveService.applyLeave(request, userId);

        assertEquals(2, responses.size(), "Should only return responses for the 2 new days");

        List<LocalDate> responseDates = responses.stream().map(CreateLeaveResponse::getDate).toList();
        assertTrue(responseDates.contains(dayOne));
        assertTrue(responseDates.contains(dayThree));
        assertFalse(responseDates.contains(dayTwo));

        ArgumentCaptor<Leave> leaveCaptor = ArgumentCaptor.forClass(Leave.class);
        verify(leaveRepository, times(2)).save(leaveCaptor.capture());

        List<LocalDate> savedDates = leaveCaptor.getAllValues().stream().map(Leave::getDate).toList();
        assertTrue(savedDates.containsAll(Arrays.asList(dayOne, dayThree)));
        assertFalse(savedDates.contains(dayTwo));
    }

    @Test
    void shouldThrowBadRequestWhenAllDatesAreFromPreviousMonth() {
        CreateLeaveRequest request = new CreateLeaveRequest(
                leaveCategoryId,
                List.of(LocalDate.now().minusMonths(1)),
                DurationType.FULL_DAY,
                LocalTime.of(9, 0),
                "Test description"
        );

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(createValidLeaveCategory());

        assertThrows(HttpException.class, () -> leaveService.applyLeave(request, userId));
    }

    @Test
    void shouldThrowBadRequestWhenAllDatesAreFromNextYear() {
        CreateLeaveRequest request = new CreateLeaveRequest(
                leaveCategoryId,
                List.of(LocalDate.now().plusYears(1)),
                DurationType.FULL_DAY,
                LocalTime.of(9, 0),
                "Test description"
        );

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(createValidLeaveCategory());

        assertThrows(HttpException.class, () -> leaveService.applyLeave(request, userId));
    }

    @Test
    void shouldThrowBadRequestWhenAllValidDatesAreWeekends() {
        CreateLeaveRequest request = new CreateLeaveRequest(
                leaveCategoryId,
                List.of(LocalDate.of(2026, 4, 4), LocalDate.of(2026, 4, 5)), // Sat & Sun
                DurationType.FULL_DAY,
                LocalTime.of(9, 0),
                "Test description"
        );

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(createValidLeaveCategory());

        assertThrows(HttpException.class, () -> leaveService.applyLeave(request, userId));
    }

    @Test
    void shouldThrowConflictWhenAllDatesAlreadyApplied() {
        User user = createValidUser();
        LeaveCategory category = createValidLeaveCategory();
        LocalDate today = LocalDate.now();

        Leave existingLeave = new Leave();
        existingLeave.setDate(today);
        existingLeave.setUser(user);

        CreateLeaveRequest request = new CreateLeaveRequest(
                leaveCategoryId,
                List.of(today),
                DurationType.FULL_DAY,
                LocalTime.of(9, 0),
                "Test description"
        );

        when(userService.getUserByUserId(userId)).thenReturn(user);
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(category);
        when(leaveRepository.findAllByUserId(eq(userId), any(Sort.class))).thenReturn(List.of(existingLeave));

        assertThrows(HttpException.class, () -> leaveService.applyLeave(request, userId));
    }
}
