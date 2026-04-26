package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.LeaveResponse;
import com.technogise.leave_management_system.dto.UpdateLeaveRequest;
import com.technogise.leave_management_system.dto.UpdateLeaveResponse;
import com.technogise.leave_management_system.dto.CreateLeaveResponse;
import com.technogise.leave_management_system.constants.LeaveConstants;
import com.technogise.leave_management_system.dto.CreateLeaveRequest;
import com.technogise.leave_management_system.entity.Holiday;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.HolidayType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.handler.LeaveIntegrationHandler;
import com.technogise.leave_management_system.repository.HolidayRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.ZoneId;
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

    @Mock
    private AnnualLeaveService annualLeaveService;

    @Mock
    private LeaveIntegrationHandler leaveIntegrationHandler;

    @Mock
    private HolidayService holidayService;

    @InjectMocks
    private LeaveService leaveService;

    @Mock
    private LeaveIntegrationHandler integrationHandler;

    private UUID userId;
    private UUID leaveCategoryId;
    private UUID holidayId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        leaveCategoryId = UUID.randomUUID();
        holidayId = UUID.randomUUID();
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
        leaveCategory.setName(LeaveConstants.ANNUAL_LEAVE);
        leaveCategory.setCreatedAt(LocalDateTime.now());
        leaveCategory.setUpdatedAt(LocalDateTime.now());
        return leaveCategory;
    }

    private Holiday createOptionalHoliday() {
        Holiday holiday = new Holiday();
        holiday.setId(UUID.randomUUID());
        holiday.setName("Diwali");
        holiday.setType(HolidayType.OPTIONAL);
        holiday.setDate(LocalDate.of(2026, 11, 9));
        holiday.setCreatedAt(LocalDateTime.of(2026, Month.JANUARY, 1, 0, 0, 0));
        holiday.setUpdatedAt(LocalDateTime.of(2026, Month.JANUARY, 1, 0, 0, 0));
        return holiday;
    }

    private Holiday createFixedHoliday() {
        Holiday holiday = new Holiday();
        holiday.setId(holidayId);
        holiday.setName("Independence Day");
        holiday.setType(HolidayType.FIXED);
        holiday.setDate(LocalDate.of(2026, 8, 15));
        holiday.setCreatedAt(LocalDateTime.of(2026, Month.JANUARY, 1, 0, 0, 0));
        holiday.setUpdatedAt(LocalDateTime.of(2026, Month.JANUARY, 1, 0, 0, 0));
        return holiday;
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

    private CreateLeaveRequest createValidOptionalHolidayLeaveRequest() {
        CreateLeaveRequest request = new CreateLeaveRequest();
        Holiday optionalHoliday = createOptionalHoliday();
        request.setHolidayId(holidayId);
        request.setDates(List.of(nextWeekday()));
        request.setDuration(DurationType.FULL_DAY);
        request.setStartTime(LocalTime.of(10, 0, 0));
        request.setDescription(optionalHoliday.getName());

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
        request.setStartTime(LocalTime.of(9, 0));
        request.setDescription("Updated description");
        request.setLeaveCategoryId(leaveCategoryId);
        return request;
    }
    @Test
    void shouldReturnEmployeeLeavesWhenEmployeeRequestsLeavesWithScopeSelf() {
        User employee = createEmployee();
        Leave employeeLeave = createEmployeeLeave(employee, createLeaveCategory());
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(employee.getId(), Sort.by(Sort.Direction.DESC, "date")))
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
        when(leaveRepository.findAllByDeletedAtIsNull(Sort.by(Sort.Direction.DESC, "date")))
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
    void shouldThrowBadRequestWhenEmpIdIsProvidedWithScopeSelf() {
        User manager = createManager();
        UUID empId = UUID.randomUUID();
        when(userService.getUserByUserId(manager.getId())).thenReturn(manager);

        HttpException ex = assertThrows(HttpException.class, () ->
                leaveService.getAllLeaves(manager.getId(), "SELF", null, empId, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("empId can only be used when scope is ORGANIZATION", ex.getMessage());
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
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(employee.getId(), Sort.by(Sort.Direction.DESC, "date")))
                .thenReturn(List.of(employeeLeave));

        List<LeaveResponse> result = leaveService.getAllLeaves(employee.getId(), "self", null,null ,null);

        assertEquals(1, result.size());
        assertEquals("Employee", result.getFirst().employeeName);
    }

    @Test
    void shouldNotApplyStatusFilterWhenStatusIsBlank() {
        User employee = createEmployee();
        Leave employeeLeave = createEmployeeLeave(employee, createLeaveCategory());
        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(employee.getId(), Sort.by(Sort.Direction.DESC, "date")))
                .thenReturn(List.of(employeeLeave));

        List<LeaveResponse> result = leaveService.getAllLeaves(employee.getId(), "self", "", null ,null);

        assertEquals(1, result.size());
        assertEquals("Employee", result.getFirst().employeeName);
    }

    @Test
    void shouldReturnEmployeeUpcomingLeaveWhenStatusIsUpcomingAndScopeIsSelf() {
        User employee = createEmployee();
        Leave employeeLeave = createEmployeeLeave(employee, createLeaveCategory());
        employeeLeave.setDate(LocalDate.now().plusDays(1));
        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(employee.getId(), Sort.by(Sort.Direction.DESC, "date")))
                .thenReturn(List.of(employeeLeave));

        List<LeaveResponse> result = leaveService.getAllLeaves(employee.getId(), "self", "upcoming", null,null);

        assertEquals(1, result.size());
        assertEquals("Employee", result.getFirst().employeeName);
    }

    @Test
    void shouldReturnEmployeeLeavesForGivenYearWhenManagerProvidesEmpIdAndYear() {
        User manager = createManager();
        User employee = createEmployee();
        LeaveCategory category = createLeaveCategory();
        Leave leave = createEmployeeLeave(employee, category);
        leave.setDate(LocalDate.of(2024, 6, 15));

        LocalDate startDate = LocalDate.of(2024, 1, 1);
        LocalDate endDate = LocalDate.of(2024, 12, 31);

        when(userService.getUserByUserId(manager.getId())).thenReturn(manager);
        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(leaveRepository.findAllByUserIdAndDateBetweenAndDeletedAtIsNull(
                employee.getId(), startDate, endDate,
                Sort.by(Sort.Direction.DESC, "date")))
                .thenReturn(List.of(leave));

        List<LeaveResponse> result = leaveService.getAllLeaves(
                manager.getId(), "ORGANIZATION", null, employee.getId(), 2024);

        assertEquals(1, result.size());
        assertEquals(employee.getName(), result.getFirst().employeeName);
        assertEquals(LocalDate.of(2024, 6, 15), result.getFirst().date);
    }

    @Test
    void shouldThrowForbiddenWhenNonManagerRequestsLeavesByEmployeeAndYear() {
        User employee = createEmployee();
        UUID targetEmpId = UUID.randomUUID();
        Integer targetYear = 2024;

        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);

        HttpException exception = assertThrows(HttpException.class, () ->
                leaveService.getAllLeaves(employee.getId(), "ORGANIZATION", null, targetEmpId, targetYear)
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Not allowed to access this resource", exception.getMessage());

        verify(leaveRepository, never()).findAllByUserIdAndDateBetweenAndDeletedAtIsNull(any(), any(), any(), any());
    }

    @Test
    void shouldDefaultToCurrentYearWhenYearIsNotProvided() {
        User manager = createManager();
        User employee = createEmployee();
        LeaveCategory category = createLeaveCategory();
        Leave leave = createEmployeeLeave(employee, category);

        int currentYear = LocalDate.now().getYear();
        LocalDate startDate = LocalDate.of(currentYear, 1, 1);
        LocalDate endDate = LocalDate.of(currentYear, 12, 31);

        when(userService.getUserByUserId(manager.getId())).thenReturn(manager);
        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(leaveRepository.findAllByUserIdAndDateBetweenAndDeletedAtIsNull(
                employee.getId(), startDate, endDate,
                Sort.by(Sort.Direction.DESC, "date")))
                .thenReturn(List.of(leave));

        List<LeaveResponse> result = leaveService.getAllLeaves(
                manager.getId(), "ORGANIZATION", null, employee.getId(), null);

        assertEquals(1, result.size());
        verify(leaveRepository).findAllByUserIdAndDateBetweenAndDeletedAtIsNull(
                employee.getId(), startDate, endDate,
                Sort.by(Sort.Direction.DESC, "date"));
    }

    @Test
    void shouldThrowNotFoundWhenUserDoesNotExist() {
        User employee = createEmployee();
        when(userService.getUserByUserId(employee.getId()))
                .thenThrow(new HttpException(org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));

        assertThrows(HttpException.class, () ->
                leaveService.getAllLeaves(employee.getId(), "self", null, null ,null));
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
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId), any(Sort.class))).thenReturn(List.of());
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
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId), any(Sort.class))).thenReturn(List.of());
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
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId), any(Sort.class))).thenReturn(List.of());
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
                null,
                Arrays.asList(dayOne, dayTwo, dayThree),
                DurationType.FULL_DAY,
                LocalTime.of(9, 0),
                "Description"
        );

        when(userService.getUserByUserId(userId)).thenReturn(user);
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(category);
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId), any(Sort.class)))
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
                null,
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
                null,
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
                null,
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
                null,
                List.of(weekday),
                DurationType.FULL_DAY,
                LocalTime.of(9, 0),
                "Test description"
        );

        when(userService.getUserByUserId(userId)).thenReturn(user);
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(category);
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId), any(Sort.class))).thenReturn(List.of(existingLeave));

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
        leave.setDuration(DurationType.FULL_DAY);
        leave.setLeaveCategory(createValidLeaveCategory());

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
        leave.setDuration(DurationType.FULL_DAY);
        leave.setLeaveCategory(createValidLeaveCategory());

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
        leave.setDuration(DurationType.FULL_DAY);
        leave.setLeaveCategory(createValidLeaveCategory());

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
        leaveBeingUpdated.setDuration(DurationType.FULL_DAY);
        leaveBeingUpdated.setLeaveCategory(createValidLeaveCategory());

        UpdateLeaveRequest request = createValidUpdateRequest();
        request.setDate(newDate);

        when(leaveRepository.findById(leaveBeingUpdated.getId()))
                .thenReturn(Optional.of(leaveBeingUpdated));
        when(leaveRepository.existsByUserIdAndDateAndIdNotAndDeletedAtIsNull(userId, newDate, leaveBeingUpdated.getId()))
                .thenReturn(true);

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.updateLeave(leaveBeingUpdated.getId(), request, userId));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }


    @Test
    void shouldNotThrowConflictWhenNewDateIsSameAsExistingLeaveDate() {
        User user = createValidUser();
        LocalDate sameDate = nextWeekday();

        Leave leaveBeingUpdated = new Leave();
        leaveBeingUpdated.setId(UUID.randomUUID());
        leaveBeingUpdated.setUser(user);
        leaveBeingUpdated.setDate(sameDate);
        leaveBeingUpdated.setLeaveCategory(createValidLeaveCategory());
        leaveBeingUpdated.setDuration(DurationType.FULL_DAY);

        UpdateLeaveRequest request = createValidUpdateRequest();
        request.setDate(sameDate);

        when(leaveRepository.findById(leaveBeingUpdated.getId()))
                .thenReturn(Optional.of(leaveBeingUpdated));
        when(leaveRepository.existsByUserIdAndDateAndIdNotAndDeletedAtIsNull(userId, sameDate, leaveBeingUpdated.getId()))
                .thenReturn(false);
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
        when(leaveRepository.existsByUserIdAndDateAndIdNotAndDeletedAtIsNull(userId, newDate, leaveBeingUpdated.getId()))
                .thenReturn(false);
        when(leaveCategoryService.getLeaveCategoryById(any()))
                .thenReturn(category);
        when(leaveRepository.save(any(Leave.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateLeaveResponse response = leaveService.updateLeave(
                leaveBeingUpdated.getId(), request, userId);

        assertNotNull(response);
        assertEquals(newDate, response.getDate());
        assertEquals(category.getName(), response.getType());
        assertEquals(request.getDuration(), response.getDuration());
        assertEquals(request.getStartTime(), response.getStartTime());
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
        leaveBeingUpdated.setDate(nextWeekday());
        leaveBeingUpdated.setLeaveCategory(oldCategory);
        leaveBeingUpdated.setDuration(DurationType.FULL_DAY);

        UpdateLeaveRequest request = createValidUpdateRequest();
        request.setDate(nextWeekday());
        request.setLeaveCategoryId(newCategoryId);

        when(leaveRepository.findById(leaveBeingUpdated.getId()))
                .thenReturn(Optional.of(leaveBeingUpdated));
        when(leaveRepository.existsByUserIdAndDateAndIdNotAndDeletedAtIsNull(userId, request.getDate(), leaveBeingUpdated.getId()))
                .thenReturn(false);
        when(leaveCategoryService.getLeaveCategoryById(newCategoryId))
                .thenReturn(newCategory);
        when(leaveRepository.save(any(Leave.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UpdateLeaveResponse response = leaveService.updateLeave(
                leaveBeingUpdated.getId(), request, userId);

        assertEquals("Casual Leave", response.getType());
        verify(leaveCategoryService).getLeaveCategoryById(newCategoryId);
    }

    @Test
    void shouldUpdateOnlyDescriptionWhenOnlyDescriptionIsProvided() {
        User user = createValidUser();
        LeaveCategory category = createValidLeaveCategory();

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setDate(LocalDate.now().plusDays(3));
        leave.setLeaveCategory(category);
        leave.setDuration(DurationType.FULL_DAY);
        leave.setStartTime(LocalTime.of(9, 0));
        leave.setDescription("Original description");

        UpdateLeaveRequest request = new UpdateLeaveRequest();
        request.setDescription("Updated description");

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
        when(leaveRepository.save(any(Leave.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateLeaveResponse response = leaveService.updateLeave(leave.getId(), request, userId);

        assertEquals("Updated description", response.getDescription());
        assertEquals(category.getName(), response.getType());
        assertEquals(DurationType.FULL_DAY, response.getDuration());
        verify(leaveRepository, never()).existsByUserIdAndDateAndIdNotAndDeletedAtIsNull(any(), any(), any());
    }

    @Test
    void shouldUpdateOnlyDurationWhenOnlyDurationIsProvided() {
        User user = createValidUser();
        LeaveCategory category = createValidLeaveCategory();

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setDate(LocalDate.now().plusDays(3));
        leave.setLeaveCategory(category);
        leave.setDuration(DurationType.FULL_DAY);
        leave.setStartTime(LocalTime.of(9, 0));
        leave.setDescription("Original description");

        UpdateLeaveRequest request = new UpdateLeaveRequest();
        request.setDuration(DurationType.HALF_DAY);

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
        when(leaveRepository.save(any(Leave.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateLeaveResponse response = leaveService.updateLeave(leave.getId(), request, userId);

        assertEquals(DurationType.HALF_DAY, response.getDuration());
        assertEquals("Original description", response.getDescription());
    }

    @Test
    void shouldUpdateOnlyStartTimeWhenOnlyStartTimeIsProvided() {
        User user = createValidUser();
        LeaveCategory category = createValidLeaveCategory();

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setDate(LocalDate.now().plusDays(3));
        leave.setLeaveCategory(category);
        leave.setDuration(DurationType.FULL_DAY);
        leave.setStartTime(LocalTime.of(9, 0));
        leave.setDescription("Original description");

        UpdateLeaveRequest request = new UpdateLeaveRequest();
        request.setStartTime(LocalTime.of(11, 0));

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
        when(leaveRepository.save(any(Leave.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateLeaveResponse response = leaveService.updateLeave(leave.getId(), request, userId);

        assertEquals(LocalTime.of(11, 0), response.getStartTime());
        assertEquals(DurationType.FULL_DAY, response.getDuration());
    }

    @Test
    void shouldUpdateOnlyLeaveCategoryWhenOnlyLeaveCategoryIdIsProvided() {
        User user = createValidUser();
        LeaveCategory oldCategory = createValidLeaveCategory();

        UUID newCategoryId = UUID.randomUUID();
        LeaveCategory newCategory = new LeaveCategory();
        newCategory.setId(newCategoryId);
        newCategory.setName("Casual Leave");

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setDate(LocalDate.now().plusDays(3));
        leave.setLeaveCategory(oldCategory);
        leave.setDuration(DurationType.FULL_DAY);
        leave.setStartTime(LocalTime.of(9, 0));
        leave.setDescription("Original description");

        UpdateLeaveRequest request = new UpdateLeaveRequest();
        request.setLeaveCategoryId(newCategoryId);

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
        when(leaveCategoryService.getLeaveCategoryById(newCategoryId)).thenReturn(newCategory);
        when(leaveRepository.save(any(Leave.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateLeaveResponse response = leaveService.updateLeave(leave.getId(), request, userId);

        assertEquals("Casual Leave", response.getType());
        assertEquals("Original description", response.getDescription());
    }

    @Test
    void shouldThrowBadRequestWhenAllFieldsInUpdateRequestAreNull() {
        UpdateLeaveRequest emptyRequest = new UpdateLeaveRequest();

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.updateLeave(UUID.randomUUID(), emptyRequest, userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("At least one field must be provided to update", ex.getMessage());
    }

    @Test
    void shouldCallSyncWhenAppliedLeaveIsAnnualLeave() {
        CreateLeaveRequest request = createValidLeaveRequest();
        LeaveCategory annualLeaveCategory = new LeaveCategory();
        annualLeaveCategory.setId(leaveCategoryId);
        annualLeaveCategory.setName(LeaveConstants.ANNUAL_LEAVE);

        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(annualLeaveCategory);
        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId), any(Sort.class))).thenReturn(List.of());
        when(leaveRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        leaveService.applyLeave(request, userId);

        verify(annualLeaveService).syncOnLeaveCreated(any(User.class), eq(DurationType.FULL_DAY), eq(1), eq(LocalDate.now().getYear()));
    }

    @Test
    void shouldNotCallSyncWhenAppliedLeaveIsNotAnnualLeave() {
        CreateLeaveRequest request = createValidLeaveRequest();
        LeaveCategory sickLeaveCategory = new LeaveCategory();
        sickLeaveCategory.setId(leaveCategoryId);
        sickLeaveCategory.setName("Sick Leave");

        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(sickLeaveCategory);
        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId), any(Sort.class))).thenReturn(List.of());
        when(leaveRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        leaveService.applyLeave(request, userId);

        verify(annualLeaveService, never()).syncOnLeaveCreated(any(), any(), anyInt(), anyInt());
    }

    @Test
    void shouldDeleteLeaveSuccessfullyWhenLeaveExists() {
        User user = createValidUser();
        LeaveCategory leaveCategory = createValidLeaveCategory();
        Leave existingLeave = createEmployeeLeave(user, leaveCategory);

        when(leaveRepository.findById(existingLeave.getId())).thenReturn(Optional.of(existingLeave));

        leaveService.deleteLeave(existingLeave.getId(), user.getId());
        assertNotNull(existingLeave.getDeletedAt());
    }

    @Test
    void shouldThrowForbiddenErrorWhenUserTriesToDeleteLeaveThatDoesNotBelongToThem() {
        User user = createValidUser();
        User differentUser = createEmployee();
        LeaveCategory leaveCategory = createValidLeaveCategory();

        Leave existingLeave = createEmployeeLeave(differentUser, leaveCategory);

        when(leaveRepository.findById(existingLeave.getId())).thenReturn(Optional.of(existingLeave));

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.deleteLeave(existingLeave.getId(), user.getId()));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void shouldThrowConflictErrorWhenTryingToCancelAlreadyCancelledLeave() {
        User user = createValidUser();
        LeaveCategory leaveCategory = createValidLeaveCategory();

        Leave existingLeave = createEmployeeLeave(user, leaveCategory);
        existingLeave.setDeletedAt(existingLeave.getCreatedAt().plusDays(1));

        when(leaveRepository.findById(existingLeave.getId())).thenReturn(Optional.of(existingLeave));

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.deleteLeave(existingLeave.getId(), user.getId()));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    }

    @Test
    void shouldReturn400WhenCancellingLeaveWithPastDate() {
        User user = createValidUser();
        LeaveCategory leaveCategory = createValidLeaveCategory();
        Leave existingLeave = createEmployeeLeave(user, leaveCategory);
        existingLeave.setDate(existingLeave.getDate().minusDays(1));

        when(leaveRepository.findById(existingLeave.getId())).thenReturn(Optional.of(existingLeave));
        assertThrows(HttpException.class, () -> leaveService.deleteLeave(existingLeave.getId(), userId));
    }

    @Test
    void shouldReactivateCancelledLeaveWhenSameDateLeaveIsAppliedAgain() {
        User user = createValidUser();
        LeaveCategory category = createValidLeaveCategory();
        LocalDate weekday = nextWeekday();

        Leave cancelledLeave = new Leave();
        cancelledLeave.setId(UUID.randomUUID());
        cancelledLeave.setDate(weekday);
        cancelledLeave.setUser(user);
        cancelledLeave.setDeletedAt(LocalDateTime.now().minusDays(1));

        CreateLeaveRequest request = new CreateLeaveRequest(
                leaveCategoryId,
                null,
                List.of(weekday),
                DurationType.FULL_DAY,
                LocalTime.of(9, 0),
                "Reapplying cancelled leave"
        );

        when(userService.getUserByUserId(userId)).thenReturn(user);
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(category);
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId), any(Sort.class)))
                .thenReturn(List.of());
        when(leaveRepository.findByUserIdAndDate(userId, weekday))
                .thenReturn(Optional.of(cancelledLeave));
        when(leaveRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        List<CreateLeaveResponse> responses = leaveService.applyLeave(request, userId);

        assertEquals(1, responses.size());

        ArgumentCaptor<List<Leave>> captor = ArgumentCaptor.forClass(List.class);
        verify(leaveRepository).saveAll(captor.capture());

        Leave saved = captor.getValue().getFirst();
        assertNull(saved.getDeletedAt());
        assertEquals(cancelledLeave.getId(), saved.getId());
    }

    @Test
    void shouldNotThrowConflictWhenUpdatingToADateThatHasACancelledLeave() {
        User user = createValidUser();
        LocalDate newDate = nextWeekday();

        Leave leaveBeingUpdated = new Leave();
        leaveBeingUpdated.setId(UUID.randomUUID());
        leaveBeingUpdated.setUser(user);
        leaveBeingUpdated.setDate(LocalDate.now().plusDays(3));
        leaveBeingUpdated.setLeaveCategory(createValidLeaveCategory());

        UpdateLeaveRequest request = createValidUpdateRequest();
        request.setDate(newDate);

        when(leaveRepository.findById(leaveBeingUpdated.getId()))
                .thenReturn(Optional.of(leaveBeingUpdated));
        when(leaveRepository.existsByUserIdAndDateAndIdNotAndDeletedAtIsNull(userId, newDate, leaveBeingUpdated.getId()))
                .thenReturn(false);
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId))
                .thenReturn(createValidLeaveCategory());
        when(leaveRepository.save(any(Leave.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() ->
                leaveService.updateLeave(leaveBeingUpdated.getId(), request, userId));
    }

    @Test
    void shouldThrowErrorWhenLeaveToDeleteIsNotFound() {
        UUID unknownLeaveId = UUID.randomUUID();
        when(leaveRepository.findById(unknownLeaveId)).thenReturn(Optional.empty());

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.deleteLeave(unknownLeaveId, userId));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Leave not found", ex.getMessage());
    }

    @Test
    void shouldSyncAnnualLeaveBalanceWhenCancelledLeaveIsAnnualLeave() {
        User user = createValidUser();
        LeaveCategory annualLeaveCategory = new LeaveCategory();
        annualLeaveCategory.setId(UUID.randomUUID());
        annualLeaveCategory.setName(LeaveConstants.ANNUAL_LEAVE);

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setLeaveCategory(annualLeaveCategory);
        leave.setDate(LocalDate.now().plusDays(1));
        leave.setDuration(DurationType.FULL_DAY);

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));

        leaveService.deleteLeave(leave.getId(), user.getId());

        verify(annualLeaveService).syncOnLeaveDeleted(user, DurationType.FULL_DAY, leave.getDate().getYear());
    }

    @Test
    void shouldNotSyncAnnualLeaveBalanceWhenCancelledLeaveIsNotAnnualLeave() {
        User user = createValidUser();
        LeaveCategory sickLeaveCategory = new LeaveCategory();
        sickLeaveCategory.setId(UUID.randomUUID());
        sickLeaveCategory.setName("Sick Leave");

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setLeaveCategory(sickLeaveCategory);
        leave.setDate(LocalDate.now().plusDays(1));
        leave.setDuration(DurationType.FULL_DAY);

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));

        leaveService.deleteLeave(leave.getId(), user.getId());

        verify(annualLeaveService, never()).syncOnLeaveDeleted(any(), any(), anyInt());
    }

    @Test
    void shouldApplyOptionalHolidayLeaveWhenValidHolidayIdIsProvided() {
        ReflectionTestUtils.setField(leaveService, "maxOptionalHolidayDays", 2);

        CreateLeaveRequest request = createValidOptionalHolidayLeaveRequest();
        Holiday optionalHoliday = createOptionalHoliday();
        User user = createValidUser();

        when(holidayService.getHolidayById(holidayId)).thenReturn(optionalHoliday);
        when(userService.getUserByUserId(userId)).thenReturn(user);
        when(leaveRepository.countByUserIdAndHolidayIsNotNullAndDateBetweenAndDeletedAtIsNull(
                eq(userId), any(LocalDate.class), any(LocalDate.class))).thenReturn(1L);
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId), any(Sort.class))).thenReturn(List.of());
        when(leaveRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        leaveService.applyLeave(request, userId);

        ArgumentCaptor<List<Leave>> listCaptor = ArgumentCaptor.captor();
        verify(leaveRepository).saveAll(listCaptor.capture());

        List<Leave> savedLeaves = listCaptor.getValue();
        assertEquals(1, savedLeaves.size());

        Leave savedLeave = savedLeaves.getFirst();
        assertEquals(request.getDates().getFirst(), savedLeave.getDate());
        assertEquals(optionalHoliday, savedLeave.getHoliday());
        assertEquals(user, savedLeave.getUser());
        assertEquals(request.getDescription(), savedLeave.getDescription());
        assertEquals(request.getStartTime(), savedLeave.getStartTime());
        assertEquals(request.getDuration(), savedLeave.getDuration());
    }

    @Test
    void shouldThrowErrorIfBothLeaveCategoryIdAndHolidayIdExistInLeaveRequest() {
        CreateLeaveRequest request = createValidOptionalHolidayLeaveRequest();
        request.setLeaveCategoryId(leaveCategoryId);

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.applyLeave(request, userId));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Cannot apply for a leave with both fields provided. Provide either holidayId or leaveCategoryId.", ex.getMessage());
    }

    @Test
    void shouldThrowErrorIfBothLeaveCategoryIdAndHolidayIdAreNullInLeaveRequest() {
        CreateLeaveRequest request = createValidOptionalHolidayLeaveRequest();
        request.setHolidayId(null);

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.applyLeave(request, userId));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("At least one of the two fields must be provided holiday_id or category_id.", ex.getMessage());
    }

    @Test
    void shouldThrow400ErrorIfNumberOfOptionalHolidayForCurrentYearExceedsAllocatedNumber() {
        ReflectionTestUtils.setField(leaveService, "maxOptionalHolidayDays", 2);

        CreateLeaveRequest request = createValidOptionalHolidayLeaveRequest();
        User user = createValidUser();
        long optionalHolidaysCount = 3L;

        int currentYear = LocalDate.now(ZoneId.of("Asia/Kolkata")).getYear();
        LocalDate startDate = LocalDate.of(currentYear, 1, 1);
        LocalDate endDate = LocalDate.of(currentYear, 12, 31);

        when(userService.getUserByUserId(userId)).thenReturn(user);
        when(leaveRepository.countByUserIdAndHolidayIsNotNullAndDateBetweenAndDeletedAtIsNull(
                user.getId(),
                startDate,
                endDate
        )).thenReturn(optionalHolidaysCount);

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.applyLeave(request, userId));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Cannot apply more than allocated days for optional holidays", ex.getMessage());
    }

    @Test
    void shouldReturnHolidayTypeWhenLeaveHasHolidayInsteadOfCategory() {
        User employee = createEmployee();
        Holiday holiday = createOptionalHoliday();

        Leave holidayLeave = new Leave();
        holidayLeave.setId(UUID.randomUUID());
        holidayLeave.setUser(employee);
        holidayLeave.setLeaveCategory(null);
        holidayLeave.setHoliday(holiday);
        holidayLeave.setDate(LocalDate.now().plusDays(1));
        holidayLeave.setDuration(DurationType.FULL_DAY);
        holidayLeave.setStartTime(LocalTime.of(10, 0));
        holidayLeave.setUpdatedAt(LocalDateTime.now());

        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(employee.getId(), Sort.by(Sort.Direction.DESC, "date")))
                .thenReturn(List.of(holidayLeave));

        List<LeaveResponse> result = leaveService.getAllLeaves(employee.getId(), "self", null, null, null);

        assertEquals(1, result.size());
        assertEquals(holiday.getType().getDisplayName(), result.getFirst().type);
    }

    @Test
    void shouldNotSyncAnnualLeaveBalanceWhenCancelledLeaveIsOptionalHoliday() {
        User user = createValidUser();
        Holiday holiday = createOptionalHoliday();

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setLeaveCategory(null);
        leave.setHoliday(holiday);
        leave.setDate(LocalDate.now().plusDays(1));
        leave.setDuration(DurationType.FULL_DAY);

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));

        leaveService.deleteLeave(leave.getId(), user.getId());

        assertNotNull(leave.getDeletedAt());
        verify(annualLeaveService, never()).syncOnLeaveDeleted(any(), any(), anyInt());
    }

    @Test
    void shouldThrowBadRequestWhenApplyingLeaveOnADateThatIsAFixedHoliday() {
        LocalDate holidayDate = nextWeekday();
        User user = createValidUser();

        Holiday fixedHoliday = new Holiday();
        fixedHoliday.setId(UUID.randomUUID());
        fixedHoliday.setDate(holidayDate);
        fixedHoliday.setType(HolidayType.FIXED);

        CreateLeaveRequest request = new CreateLeaveRequest();
        request.setLeaveCategoryId(leaveCategoryId);
        request.setDates(List.of(holidayDate));
        request.setDuration(DurationType.FULL_DAY);
        when(userService.getUserByUserId(userId)).thenReturn(user);
        when(holidayService.getHolidaysByType(HolidayType.FIXED)).thenReturn(List.of(fixedHoliday));

        HttpException ex = assertThrows(HttpException.class, () ->
                leaveService.applyLeave(request, userId)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Cannot apply leave on weekends or fixed holidays", ex.getMessage());
    }

    @Test
    void shouldUpdateRegularLeaveToOptionalHolidaySuccessfully() {
        User user = createValidUser();
        Holiday holiday = createOptionalHoliday();
        LocalDate mockDate = LocalDate.of(2026, 4,27);

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setDate(mockDate);
        leave.setLeaveCategory(createValidLeaveCategory());
        leave.setDuration(DurationType.FULL_DAY);
        leave.setStartTime(LocalTime.of(10, 0));
        leave.setDescription("Regular leave");

        UpdateLeaveRequest request = new UpdateLeaveRequest();
        request.setHolidayId(holidayId);

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
        when(holidayService.getHolidayById(holidayId)).thenReturn(holiday);
        when(leaveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateLeaveResponse response = leaveService.updateLeave(leave.getId(), request, userId);

        assertEquals("OPTIONAL HOLIDAY", response.getType());
        assertNull(leave.getLeaveCategory());
        assertEquals(holiday, leave.getHoliday());
    }

    @Test
    void shouldUpdateOptionalHolidayToRegularLeaveSuccessfully() {
        User user = createValidUser();
        Holiday holiday = createOptionalHoliday();
        LeaveCategory newCategory = createValidLeaveCategory();

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setDate(holiday.getDate());
        leave.setLeaveCategory(null);
        leave.setHoliday(holiday);
        leave.setDuration(DurationType.FULL_DAY);
        leave.setStartTime(LocalTime.of(10, 0));
        leave.setDescription(holiday.getName());

        UpdateLeaveRequest request = new UpdateLeaveRequest();
        request.setLeaveCategoryId(leaveCategoryId);

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(newCategory);
        when(leaveRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UpdateLeaveResponse response = leaveService.updateLeave(leave.getId(), request, userId);

        assertEquals(newCategory.getName(), response.getType());
        assertNull(leave.getHoliday());
        assertEquals(newCategory, leave.getLeaveCategory());
    }

    @Test
    void shouldThrowBadRequestWhenBothHolidayIdAndCategoryIdProvidedInUpdateRequest() {
        User user = createValidUser();

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setDate(nextWeekday());
        leave.setLeaveCategory(createValidLeaveCategory());
        leave.setDuration(DurationType.FULL_DAY);

        UpdateLeaveRequest request = new UpdateLeaveRequest();
        request.setHolidayId(holidayId);
        request.setLeaveCategoryId(leaveCategoryId);

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.updateLeave(leave.getId(), request, userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Cannot apply for a leave with both fields provided. Provide either holidayId or leaveCategoryId.", ex.getMessage());
    }

    @Test
    void shouldThrow400ErrorWhenUpdatingRegularLeaveToOptionalHolidayExceedsAllocatedQuota() {
        ReflectionTestUtils.setField(leaveService, "maxOptionalHolidayDays", 2);

        User user = createValidUser();

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setDate(nextWeekday());
        leave.setLeaveCategory(createValidLeaveCategory());
        leave.setDuration(DurationType.FULL_DAY);

        UpdateLeaveRequest request = new UpdateLeaveRequest();
        request.setHolidayId(holidayId);

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
        when(userService.getUserByUserId(userId)).thenReturn(user);
        when(leaveRepository.countByUserIdAndHolidayIsNotNullAndDateBetweenAndDeletedAtIsNull(
                eq(userId), any(), any())).thenReturn(2L);

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.updateLeave(leave.getId(), request, userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Cannot apply more than allocated days for optional holidays", ex.getMessage());
    }
}
