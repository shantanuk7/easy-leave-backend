package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.*;
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
import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

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

    private LocalDate nextWeekday() {
        LocalDate date = LocalDate.now();
        while (leaveService.isWeekendDay(date)) {
            date = date.plusDays(1);
        }
        return date;
    }

    private List<LocalDate> nextWeekdays(int count) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate date = LocalDate.now();
        while (dates.size() < count) {
            if (!leaveService.isWeekendDay(date)) {
                dates.add(date);
            }
            date = date.plusDays(1);
        }
        return dates;
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
        request.setDates(List.of(nextWeekday()));
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

    private UpdateLeaveRequest createValidUpdateRequest() {
        UpdateLeaveRequest request = new UpdateLeaveRequest();
        request.setDate(LocalDate.now().plusDays(3));
        request.setDuration(DurationType.FULL_DAY);
        request.setTime(LocalTime.of(9, 0));
        request.setDescription("Updated description");
        request.setLeaveCategoryId(leaveCategoryId);
        return request;
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
        LocalDate saturday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        assertTrue(leaveService.isWeekendDay(saturday));
    }

    @Test
    void shouldReturnTrueForSunday() {
        LocalDate sunday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        assertTrue(leaveService.isWeekendDay(sunday));
    }

    @Test
    void shouldReturnFalseForMonday() {
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        assertFalse(leaveService.isWeekendDay(monday));
    }

    @Test
    void shouldReturnFalseForTuesday() {
        LocalDate tuesday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));
        assertFalse(leaveService.isWeekendDay(tuesday));
    }

    @Test
    void shouldSaveLeaveWithCorrectFieldsWhenRequestIsValid() {
        CreateLeaveRequest request = createValidLeaveRequest();
        LeaveCategory leaveCategory = createValidLeaveCategory();
        User user = createValidUser();

        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(leaveCategory);
        when(userService.getUserByUserId(userId)).thenReturn(user);
        when(leaveRepository.findAllByUserId(eq(userId), any(Sort.class))).thenReturn(List.of());
        when(leaveRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        leaveService.applyLeave(request, userId);

        ArgumentCaptor<List<Leave>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(leaveRepository).saveAll(listCaptor.capture());

        List<Leave> savedLeaves = listCaptor.getValue();
        assertEquals(1, savedLeaves.size());

        Leave savedLeave = savedLeaves.get(0);
        assertEquals(request.getDates().get(0), savedLeave.getDate());
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
        when(leaveRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<CreateLeaveResponse> responses = leaveService.applyLeave(request, userId);

        assertFalse(responses.isEmpty());
        assertInstanceOf(CreateLeaveResponse.class, responses.get(0));
    }

    @Test
    void shouldReturnOneResponsePerNewDateProvided() {
        CreateLeaveRequest request = createValidLeaveRequest();
        List<LocalDate> dates = nextWeekdays(3);
        request.setDates(dates);

        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(createValidLeaveCategory());
        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveRepository.findAllByUserId(eq(userId), any(Sort.class))).thenReturn(List.of());
        when(leaveRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<CreateLeaveResponse> responses = leaveService.applyLeave(request, userId);

        assertEquals(dates.size(), responses.size());
        verify(leaveRepository, times(1)).saveAll(anyList());
    }

    @Test
    void shouldSkipExistingDatesAndSaveOnlyNewOnesWhenOverlappingDatesExist() {
        User user = createValidUser();
        LeaveCategory category = createValidLeaveCategory();

        LocalDate today = LocalDate.now();
        List<LocalDate> weekdays = today.withDayOfMonth(1)
                .datesUntil(today.withDayOfMonth(today.lengthOfMonth()).plusDays(1))
                .filter(d -> !leaveService.isWeekendDay(d))
                .limit(3)
                .toList();

        LocalDate dayOne   = weekdays.get(0);
        LocalDate dayTwo   = weekdays.get(1);
        LocalDate dayThree = weekdays.get(2);

        Leave existingLeaveOnDayTwo = new Leave();
        existingLeaveOnDayTwo.setDate(dayTwo);

        CreateLeaveRequest request = new CreateLeaveRequest(
                leaveCategoryId,
                Arrays.asList(dayOne, dayTwo, dayThree),
                DurationType.FULL_DAY,
                LocalTime.of(9, 0),
                "Description"
        );

        when(userService.getUserByUserId(userId)).thenReturn(user);
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(category);
        when(leaveRepository.findAllByUserId(eq(userId), any(Sort.class)))
                .thenReturn(List.of(existingLeaveOnDayTwo));
        when(leaveRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<CreateLeaveResponse> responses = leaveService.applyLeave(request, userId);

        assertEquals(2, responses.size());
        List<LocalDate> responseDates = responses.stream().map(CreateLeaveResponse::getDate).toList();
        assertTrue(responseDates.containsAll(Arrays.asList(dayOne, dayThree)));
        assertFalse(responseDates.contains(dayTwo));

        ArgumentCaptor<List<Leave>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(leaveRepository).saveAll(listCaptor.capture());

        List<Leave> savedList = listCaptor.getValue();
        assertEquals(2, savedList.size());
        List<LocalDate> savedDates = savedList.stream().map(Leave::getDate).toList();
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
        List<LocalDate> weekends = LocalDate.now()
                .datesUntil(LocalDate.now().plusMonths(2))
                .filter(leaveService::isWeekendDay)
                .limit(2)
                .toList();

        CreateLeaveRequest request = new CreateLeaveRequest(
                leaveCategoryId,
                weekends,
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
        LocalDate weekday = nextWeekday();

        Leave existingLeave = new Leave();
        existingLeave.setDate(weekday);
        existingLeave.setUser(user);

        CreateLeaveRequest request = new CreateLeaveRequest(
                leaveCategoryId,
                List.of(weekday),
                DurationType.FULL_DAY,
                LocalTime.of(9, 0),
                "Test description"
        );

        when(userService.getUserByUserId(userId)).thenReturn(user);
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(category);
        when(leaveRepository.findAllByUserId(eq(userId), any(Sort.class))).thenReturn(List.of(existingLeave));

        assertThrows(HttpException.class, () -> leaveService.applyLeave(request, userId));
    }

    // GET /leaves/{leaveId} - leave details
    @Test
    void shouldReturnLeaveDetailsWhenLeaveExistsAndBelongsToUser() {
        User user = createValidUser();
        LeaveCategory category = createValidLeaveCategory();
        Leave leave = createEmployeeLeave(user, category);

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));

        LeaveResponse response = leaveService.getLeaveById(leave.getId(), userId);

        assertEquals(leave.getId(), response.id);
        assertEquals(leave.getDate(), response.date);
        assertEquals(user.getName(), response.employeeName);
        assertEquals(category.getName(), response.type);
        assertEquals(leave.getDuration(), response.duration);
        assertEquals(leave.getStartTime(), response.startTime);
        assertEquals(leave.getUpdatedAt(), response.applyOn);
        assertEquals(leave.getDescription(), response.reason);
    }

    @Test
    void shouldThrowNotFoundWhenLeaveDoesNotExist() {
        User user = createValidUser();
        LeaveCategory category = createValidLeaveCategory();
        Leave leave = createEmployeeLeave(user, category);

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.empty());

        HttpException exception = assertThrows(
                HttpException.class,
                () -> leaveService.getLeaveById(leave.getId(), UUID.randomUUID())
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Leave not found"));
    }

    @Test
    void shouldReturnLeaveWhenUserIsManager() {
        User employee = createEmployee();
        User manager = createManager();

        LeaveCategory category = createLeaveCategory();
        Leave leave = createEmployeeLeave(employee, category);

        when(userService.getUserByUserId(manager.getId())).thenReturn(manager);
        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));

        LeaveResponse response = leaveService.getLeaveById(leave.getId(), manager.getId());

        assertEquals(leave.getId(), response.id);
        assertEquals(leave.getDate(), response.date);
        assertEquals(employee.getName(), response.employeeName);
        assertEquals(category.getName(), response.type);
        assertEquals(leave.getDuration(), response.duration);
        assertEquals(leave.getStartTime(), response.startTime);
        assertEquals(leave.getUpdatedAt(), response.applyOn);
        assertEquals(leave.getDescription(), response.reason);
    }

    @Test
    void shouldThrowForbiddenWhenUserIsNotOwnerAndNotManager() {
        User user = createEmployee();
        User anotherUser = createEmployee();

        LeaveCategory category = createLeaveCategory();
        Leave leave = createEmployeeLeave(user, category);

        when(userService.getUserByUserId(anotherUser.getId())).thenReturn(anotherUser);
        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));

        HttpException exception = assertThrows(
                HttpException.class,
                () -> leaveService.getLeaveById(leave.getId(), anotherUser.getId())
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Not Allowed to access this resource", exception.getMessage());
    }

    @Test
    void shouldThrowNotFoundWhenLeaveIdDoesNotExist() {
        UUID unknownLeaveId = UUID.randomUUID();
        when(leaveRepository.findById(unknownLeaveId)).thenReturn(Optional.empty());

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.updateLeave(unknownLeaveId, createValidUpdateRequest(), userId));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void shouldThrowForbiddenWhenUserDoesNotExist() {
        User user = createValidUser();
        UUID differentUserId = UUID.randomUUID();

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setDate(LocalDate.now().plusDays(3));

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));


        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.updateLeave(leave.getId(), createValidUpdateRequest(), differentUserId));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void shouldThrowBadRequestWhenExistingLeaveDateIsNoLongerEditable() {
        User user = createValidUser();

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setDate(LocalDate.now().minusMonths(1));

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.updateLeave(leave.getId(), createValidUpdateRequest(), userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldThrowBadRequestWhenNewDateIsNotWithinValidRange() {
        User user = createValidUser();

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setDate(LocalDate.now().plusDays(3));

        UpdateLeaveRequest request = createValidUpdateRequest();
        request.setDate(LocalDate.now().plusYears(1));

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.updateLeave(leave.getId(), request, userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldThrowBadRequestWhenNewDateIsAWeekend() {
        User user = createValidUser();

        LocalDate nextSaturday = LocalDate.now()
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setDate(LocalDate.now().plusDays(3));

        UpdateLeaveRequest request = createValidUpdateRequest();
        request.setDate(nextSaturday);

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.updateLeave(leave.getId(), request, userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldThrowConflictWhenNewDateAlreadyHasAnotherLeave() {
        User user = createValidUser();

        LocalDate newDate = nextWeekday();

        Leave leaveBeingUpdated = new Leave();
        leaveBeingUpdated.setId(UUID.randomUUID());
        leaveBeingUpdated.setUser(user);
        leaveBeingUpdated.setDate(LocalDate.now().plusDays(3));

        Leave anotherLeaveOnSameDate = new Leave();
        anotherLeaveOnSameDate.setId(UUID.randomUUID());
        anotherLeaveOnSameDate.setUser(user);
        anotherLeaveOnSameDate.setDate(newDate);

        UpdateLeaveRequest request = createValidUpdateRequest();
        request.setDate(newDate);

        when(leaveRepository.findById(leaveBeingUpdated.getId()))
                .thenReturn(Optional.of(leaveBeingUpdated));
        when(leaveRepository.findAllByUserId(eq(userId), any(Sort.class)))
                .thenReturn(List.of(leaveBeingUpdated, anotherLeaveOnSameDate));

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.updateLeave(leaveBeingUpdated.getId(), request, userId));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void shouldNotThrowConflictWhenNewDateIsSameAsExistingLeaveDate() {
        User user = createValidUser();

        LocalDate sameDate = LocalDate.now().plusDays(3);

        Leave leaveBeingUpdated = new Leave();
        leaveBeingUpdated.setId(UUID.randomUUID());
        leaveBeingUpdated.setUser(user);
        leaveBeingUpdated.setDate(sameDate);
        leaveBeingUpdated.setLeaveCategory(createValidLeaveCategory());

        UpdateLeaveRequest request = createValidUpdateRequest();
        request.setDate(sameDate);

        when(leaveRepository.findById(leaveBeingUpdated.getId()))
                .thenReturn(Optional.of(leaveBeingUpdated));
        when(leaveRepository.findAllByUserId(eq(userId), any(Sort.class)))
                .thenReturn(List.of(leaveBeingUpdated));
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId))
                .thenReturn(createValidLeaveCategory());
        when(leaveRepository.save(any(Leave.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() ->
                leaveService.updateLeave(leaveBeingUpdated.getId(), request, userId));
    }

    @Test
    void shouldSaveUpdatedLeaveWithCorrectFieldsAndReturnResponse() {
        User user = createValidUser();
        LeaveCategory category = createValidLeaveCategory();

        LocalDate newDate = nextWeekday();

        Leave leaveBeingUpdated = new Leave();
        leaveBeingUpdated.setId(UUID.randomUUID());
        leaveBeingUpdated.setUser(user);
        leaveBeingUpdated.setDate(LocalDate.now().plusDays(3));
        leaveBeingUpdated.setLeaveCategory(category);
        leaveBeingUpdated.setDuration(DurationType.FULL_DAY);
        leaveBeingUpdated.setStartTime(LocalTime.of(9, 0));
        leaveBeingUpdated.setDescription("Original description");

        UpdateLeaveRequest request = createValidUpdateRequest();
        request.setDate(newDate);

        when(leaveRepository.findById(leaveBeingUpdated.getId()))
                .thenReturn(Optional.of(leaveBeingUpdated));
        when(leaveRepository.findAllByUserId(eq(userId), any(Sort.class)))
                .thenReturn(List.of(leaveBeingUpdated));
        when(leaveCategoryService.getLeaveCategoryById(any()))
                .thenReturn(category);
        when(leaveRepository.save(any(Leave.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateLeaveResponse response = leaveService.updateLeave(
                leaveBeingUpdated.getId(), request, userId);

        assertNotNull(response);
        assertEquals(newDate, response.getDate());
        assertEquals(category.getName(), response.getLeaveCategoryName());
        assertEquals(request.getDuration(), response.getDuration());
        assertEquals(request.getTime(), response.getStartTime());
        assertEquals(request.getDescription(), response.getDescription());

        ArgumentCaptor<Leave> captor = ArgumentCaptor.forClass(Leave.class);
        verify(leaveRepository).save(captor.capture());

        Leave saved = captor.getValue();
        assertEquals(newDate, saved.getDate());
        assertEquals(leaveBeingUpdated.getId(), saved.getId());
    }

    @Test
    void shouldUpdateCategoryWhenNewCategoryIdIsProvided() {
        User user = createValidUser();
        LeaveCategory oldCategory = createValidLeaveCategory();

        UUID newCategoryId = UUID.randomUUID();
        LeaveCategory newCategory = new LeaveCategory();
        newCategory.setId(newCategoryId);
        newCategory.setName("Casual Leave");

        Leave leaveBeingUpdated = new Leave();
        leaveBeingUpdated.setId(UUID.randomUUID());
        leaveBeingUpdated.setUser(user);
        leaveBeingUpdated.setDate(LocalDate.now().plusDays(3));
        leaveBeingUpdated.setLeaveCategory(oldCategory);

        UpdateLeaveRequest request = createValidUpdateRequest();
        request.setLeaveCategoryId(newCategoryId);

        when(leaveRepository.findById(leaveBeingUpdated.getId()))
                .thenReturn(Optional.of(leaveBeingUpdated));
        when(leaveRepository.findAllByUserId(eq(userId), any(Sort.class)))
                .thenReturn(List.of(leaveBeingUpdated));
        when(leaveCategoryService.getLeaveCategoryById(newCategoryId))
                .thenReturn(newCategory);
        when(leaveRepository.save(any(Leave.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateLeaveResponse response = leaveService.updateLeave(
                leaveBeingUpdated.getId(), request, userId);

        assertEquals("Casual Leave", response.getLeaveCategoryName());
        verify(leaveCategoryService).getLeaveCategoryById(newCategoryId);
    }
}
