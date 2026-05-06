package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.CreateLeaveRequest;
import com.technogise.leave_management_system.dto.CreateLeaveResponse;
import com.technogise.leave_management_system.dto.UpdateLeaveRequest;
import com.technogise.leave_management_system.dto.UpdateLeaveResponse;
import com.technogise.leave_management_system.dto.LeaveResponse;
import com.technogise.leave_management_system.dto.LeaveFilterRequest;
import com.technogise.leave_management_system.constants.LeaveConstants;
import com.technogise.leave_management_system.entity.Holiday;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.HolidayType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.handler.LeaveIntegrationHandler;
import com.technogise.leave_management_system.repository.LeaveRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.jpa.domain.Specification;
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

import static com.technogise.leave_management_system.enums.ScopeType.ORGANIZATION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    private UUID userId;
    private UUID leaveCategoryId;
    private Pageable pageable;
    private UUID holidayId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        leaveCategoryId = UUID.randomUUID();
        pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "date"));
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
        leaveCategory.setAllocatedDays(10);
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
        request.setType("leave");
        return request;
    }

    private LeaveFilterRequest buildFilter(String scope, String status, UUID empId, Integer year) {
        return LeaveFilterRequest.builder()
                .scope(scope)
                .status(status)
                .empId(empId)
                .year(year)
                .build();
    }

    @Test
    void shouldReturnEmployeeLeavesWhenEmployeeRequestsLeavesWithScopeSelf() {
        User employee = createEmployee();
        Leave leave = createEmployeeLeave(employee, createLeaveCategory());
        Page<Leave> leaves = new PageImpl<>(List.of(leave));

        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(leaveRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(leaves);

        Page<LeaveResponse> result = leaveService.getAllLeaves(
                employee.getId(), buildFilter("SELF", null, null, null), pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(employee.getName(), result.getContent().getFirst().employeeName);
        assertEquals("Annual Leave", result.getContent().getFirst().type);
    }

    @Test
    void shouldThrowAccessDeniedWhenEmployeeRequestsLeavesWithScopeOrganization() {
        User employee = createEmployee();
        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);

        HttpException ex = assertThrows(HttpException.class, () ->
                leaveService.getAllLeaves(
                        employee.getId(), buildFilter("ORGANIZATION", null, null, null), pageable));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("Not Allowed to access this resource", ex.getMessage());
    }

    @Test
    void shouldReturnAllEmployeeLeavesWhenManagerRequestsLeavesWithScopeOrganization() {
        User manager = createManager();
        User employee = createEmployee();
        Leave leave = createEmployeeLeave(employee, createLeaveCategory());
        Page<Leave> leaves = new PageImpl<>(List.of(leave));

        when(userService.getUserByUserId(manager.getId())).thenReturn(manager);
        when(leaveRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(leaves);

        Page<LeaveResponse> result = leaveService.getAllLeaves(
                manager.getId(), buildFilter(ORGANIZATION.toString(), null, null, null), pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(employee.getName(), result.getContent().getFirst().employeeName);
        assertEquals("Annual Leave", result.getContent().getFirst().type);
    }

    @Test
    void shouldThrowBadRequestWhenScopeParamIsInvalid() {
        User employee = createEmployee();
        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);

        HttpException ex = assertThrows(HttpException.class, () ->
                leaveService.getAllLeaves(
                        employee.getId(), buildFilter("invalid", null, null, null), pageable));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Invalid scope query parameter", ex.getMessage());
    }

    @Test
    void shouldThrowBadRequestWhenEmpIdIsProvidedWithScopeSelf() {
        User employee = createEmployee();
        UUID empId = UUID.randomUUID();
        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);

        HttpException ex = assertThrows(HttpException.class, () ->
                leaveService.getAllLeaves(
                        employee.getId(), buildFilter("SELF", null, empId, null), pageable));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        System.out.println(ex.getMessage());
        assertEquals("empId is only allowed when scope is ORGANIZATION", ex.getMessage());
    }

    @Test
    void shouldFilterByEmpIdWhenManagerProvidesEmpIdWithScopeOrganization() {
        User manager = createManager();
        User employee = createEmployee();
        Leave leave = createEmployeeLeave(employee, createLeaveCategory());
        Page<Leave> leaves = new PageImpl<>(List.of(leave));

        when(userService.getUserByUserId(manager.getId())).thenReturn(manager);
        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(leaveRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(leaves);

        Page<LeaveResponse> result = leaveService.getAllLeaves(
                manager.getId(),
                buildFilter("ORGANIZATION", null, employee.getId(), null),
                pageable
        );

        verify(userService).getUserByUserId(employee.getId());
        assertEquals(1, result.getTotalElements());
        assertEquals(employee.getName(), result.getContent().getFirst().employeeName);
    }

    @Test
    void shouldThrowNotFoundWhenEmpIdDoesNotExist() {
        User manager = createManager();
        UUID invalidEmpId = UUID.randomUUID();

        when(userService.getUserByUserId(manager.getId())).thenReturn(manager);
        when(userService.getUserByUserId(invalidEmpId))
                .thenThrow(new HttpException(HttpStatus.NOT_FOUND, "User not found"));

        HttpException ex = assertThrows(HttpException.class, () ->
                leaveService.getAllLeaves(
                        manager.getId(),
                        buildFilter("ORGANIZATION", null, invalidEmpId, null),
                        pageable
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("User not found", ex.getMessage());
        verify(leaveRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldReturnUpcomingLeavesWhenStatusIsUpcoming() {
        User employee = createEmployee();
        Leave leave = createEmployeeLeave(employee, createLeaveCategory());
        leave.setDate(LocalDate.now().plusDays(1));
        Page<Leave> leaves = new PageImpl<>(List.of(leave));

        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(leaveRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(leaves);

        Page<LeaveResponse> result = leaveService.getAllLeaves(
                employee.getId(), buildFilter("self", "upcoming", null, null), pageable);

        assertEquals(1, result.getTotalElements());
        assertTrue(leaves.getContent().getFirst().getDate().isAfter(LocalDate.now()));
    }

    @Test
    void shouldReturnOngoingLeavesWhenStatusIsOngoing() {
        User employee = createEmployee();
        Leave leave = createEmployeeLeave(employee, createLeaveCategory());
        Page<Leave> leaves = new PageImpl<>(List.of(leave));

        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(leaveRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(leaves);

        Page<LeaveResponse> result = leaveService.getAllLeaves(
                employee.getId(), buildFilter("self", "ongoing", null, null), pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(leaves.getContent().getFirst().getDate(), LocalDate.now());
    }

    @Test
    void shouldReturnPastLeavesWhenStatusIsCompleted() {
        User employee = createEmployee();
        Leave leave = createEmployeeLeave(employee, createLeaveCategory());
        leave.setDate(LocalDate.now().minusDays(1));
        Page<Leave> leaves = new PageImpl<>(List.of(leave));

        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(leaveRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(leaves);

        Page<LeaveResponse> result = leaveService.getAllLeaves(
                employee.getId(), buildFilter("self", "completed", null, null), pageable);

        assertEquals(1, result.getTotalElements());
        assertTrue(leaves.getContent().getFirst().getDate().isBefore(LocalDate.now()));
    }

    @Test
    void shouldNotApplyStatusFilterWhenStatusIsNull() {
        User employee = createEmployee();
        Leave leave = createEmployeeLeave(employee, createLeaveCategory());
        Page<Leave> leaves = new PageImpl<>(List.of(leave));

        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(leaveRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(leaves);

        Page<LeaveResponse> result = leaveService.getAllLeaves(
                employee.getId(), buildFilter("self", null, null, null), pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Employee", result.getContent().getFirst().employeeName);
    }

    @Test
    void shouldNotApplyStatusFilterWhenStatusIsBlank() {
        User employee = createEmployee();
        Leave leave = createEmployeeLeave(employee, createLeaveCategory());
        Page<Leave> leaves = new PageImpl<>(List.of(leave));

        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(leaveRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(leaves);

        Page<LeaveResponse> result = leaveService.getAllLeaves(
                employee.getId(), buildFilter("self", "", null, null), pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals("Employee", result.getContent().getFirst().employeeName);
    }

    @Test
    void shouldReturnEmployeeLeavesForGivenYearWhenManagerProvidesEmpIdAndYear() {
        User manager = createManager();
        User employee = createEmployee();
        Leave leave = createEmployeeLeave(employee, createLeaveCategory());
        Page<Leave> leaves = new PageImpl<>(List.of(leave));

        when(userService.getUserByUserId(manager.getId())).thenReturn(manager);
        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(leaveRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(leaves);

        Page<LeaveResponse> result = leaveService.getAllLeaves(
                manager.getId(),
                buildFilter("ORGANIZATION", null, employee.getId(), 2026),
                pageable
        );

        verify(userService).getUserByUserId(employee.getId());
        assertEquals(1, result.getTotalElements());
        assertEquals(employee.getName(), result.getContent().getFirst().employeeName);
    }

    @Test
    void shouldThrowForbiddenWhenNonManagerRequestsLeavesByEmployeeAndYear() {
        User employee = createEmployee();
        UUID targetEmpId = UUID.randomUUID();

        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);

        HttpException ex = assertThrows(HttpException.class, () ->
                leaveService.getAllLeaves(
                        employee.getId(),
                        buildFilter("ORGANIZATION", null, targetEmpId, 2026),
                        pageable)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(leaveRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldApplyYearFilterWhenYearIsProvided() {
        User manager = createManager();
        User employee = createEmployee();
        Leave leave = createEmployeeLeave(employee, createLeaveCategory());
        Page<Leave> leavePage = new PageImpl<>(List.of(leave));

        when(userService.getUserByUserId(manager.getId())).thenReturn(manager);
        when(leaveRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(leavePage);

        Page<LeaveResponse> result = leaveService.getAllLeaves(
                manager.getId(), buildFilter("ORGANIZATION", null, null, 2026), pageable);

        assertEquals(1, result.getTotalElements());
        verify(leaveRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void shouldThrowNotFoundWhenUserDoesNotExist() {
        User employee = createEmployee();
        when(userService.getUserByUserId(employee.getId()))
                .thenThrow(new HttpException(HttpStatus.NOT_FOUND, "User not found"));

        assertThrows(HttpException.class, () ->
                leaveService.getAllLeaves(
                        employee.getId(), buildFilter("self", null, null, null), pageable));
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
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId))).thenReturn(List.of());
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
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId))).thenReturn(List.of());
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
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId))).thenReturn(List.of());
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
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId)))
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
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId))).thenReturn(List.of(existingLeave));

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
        LeaveCategory category = createValidLeaveCategory();

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setDate(LocalDate.now().plusDays(3));
        leave.setDuration(DurationType.FULL_DAY);
        leave.setLeaveCategory(category);

        UpdateLeaveRequest request = createValidUpdateRequest();
        request.setDate(LocalDate.now().plusYears(1));

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
        // satisfying balance check dependencies
        when(leaveCategoryService.getLeaveCategoryById(any())).thenReturn(category);
        when(leaveRepository.findAllByUserIdAndDateBetweenAndDeletedAtIsNull(any(), any(), any(), any())).thenReturn(List.of());

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.updateLeave(leave.getId(), request, userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldThrowBadRequestWhenNewDateIsAWeekend() {
        User user = createValidUser();
        LocalDate nextSaturday = LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
        LeaveCategory category = createValidLeaveCategory();

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setDate(LocalDate.now().plusDays(3));
        leave.setDuration(DurationType.FULL_DAY);
        leave.setLeaveCategory(category);

        UpdateLeaveRequest request = createValidUpdateRequest();
        request.setDate(nextSaturday);

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
        when(leaveCategoryService.getLeaveCategoryById(any())).thenReturn(category);
        when(leaveRepository.findAllByUserIdAndDateBetweenAndDeletedAtIsNull(any(), any(), any(), any())).thenReturn(List.of());

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.updateLeave(leave.getId(), request, userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldThrowConflictWhenNewDateAlreadyHasAnotherLeave() {
        User user = createValidUser();
        LocalDate newDate = nextWeekday();
        LeaveCategory category = createValidLeaveCategory();

        Leave leaveBeingUpdated = new Leave();
        leaveBeingUpdated.setId(UUID.randomUUID());
        leaveBeingUpdated.setUser(user);
        leaveBeingUpdated.setDate(LocalDate.now().plusDays(3));
        leaveBeingUpdated.setDuration(DurationType.FULL_DAY);
        leaveBeingUpdated.setLeaveCategory(category);

        UpdateLeaveRequest request = createValidUpdateRequest();
        request.setDate(newDate);

        when(leaveRepository.findById(leaveBeingUpdated.getId())).thenReturn(Optional.of(leaveBeingUpdated));
        when(leaveCategoryService.getLeaveCategoryById(any())).thenReturn(category);
        when(leaveRepository.findAllByUserIdAndDateBetweenAndDeletedAtIsNull(any(), any(), any(), any())).thenReturn(List.of());

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
        assertEquals(category.getName(), response.getLeaveCategoryName());
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
        newCategory.setAllocatedDays(10);

        Leave leaveBeingUpdated = new Leave();
        leaveBeingUpdated.setId(UUID.randomUUID());
        leaveBeingUpdated.setUser(user);
        leaveBeingUpdated.setDate(nextWeekday());
        leaveBeingUpdated.setLeaveCategory(oldCategory);
        leaveBeingUpdated.setDuration(DurationType.FULL_DAY);

        UpdateLeaveRequest request = createValidUpdateRequest();
        request.setDate(nextWeekday());
        request.setLeaveCategoryId(newCategoryId);

        when(leaveRepository.findById(leaveBeingUpdated.getId())).thenReturn(Optional.of(leaveBeingUpdated));
        when(leaveCategoryService.getLeaveCategoryById(newCategoryId)).thenReturn(newCategory);

        when(leaveRepository.findAllByUserIdAndDateBetweenAndDeletedAtIsNull(any(), any(), any(), any())).thenReturn(List.of());

        when(leaveRepository.existsByUserIdAndDateAndIdNotAndDeletedAtIsNull(userId, request.getDate(), leaveBeingUpdated.getId()))
                .thenReturn(false);
        when(leaveRepository.save(any(Leave.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateLeaveResponse response = leaveService.updateLeave(leaveBeingUpdated.getId(), request, userId);

        assertEquals("Casual Leave", response.getLeaveCategoryName());
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
        assertEquals(category.getName(), response.getLeaveCategoryName());
        assertEquals(DurationType.FULL_DAY, response.getDuration());
        verify(leaveRepository, never()).existsByUserIdAndDateAndIdNotAndDeletedAtIsNull(any(), any(), any());
    }

    @Test
    void shouldUpdateOnlyDurationWhenOnlyDurationIsProvided() {
        User user = createValidUser();
        LeaveCategory category = new LeaveCategory();
        category.setId(UUID.randomUUID());
        category.setName(LeaveConstants.ANNUAL_LEAVE);

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
        assertEquals(LeaveConstants.ANNUAL_LEAVE, response.getLeaveCategoryName());
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
        newCategory.setAllocatedDays(10);

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
        when(leaveRepository.findAllByUserIdAndDateBetweenAndDeletedAtIsNull(any(), any(), any(), any())).thenReturn(List.of());
        when(leaveRepository.save(any(Leave.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateLeaveResponse response = leaveService.updateLeave(leave.getId(), request, userId);

        assertEquals("Casual Leave", response.getLeaveCategoryName());
        assertEquals("Original description", response.getDescription());
    }

    @Test
    void shouldThrowBadRequestWhenAllFieldsInUpdateRequestAreNull() {
        UpdateLeaveRequest emptyRequest = new UpdateLeaveRequest();

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.updateLeave(UUID.randomUUID(), emptyRequest, userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Update at least one detail to save changes", ex.getMessage());
    }

    @Test
    void shouldCallSyncWhenAppliedLeaveIsAnnualLeave() {
        CreateLeaveRequest request = createValidLeaveRequest();
        LeaveCategory annualLeaveCategory = new LeaveCategory();
        annualLeaveCategory.setId(leaveCategoryId);
        annualLeaveCategory.setName(LeaveConstants.ANNUAL_LEAVE);

        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(annualLeaveCategory);
        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId))).thenReturn(List.of());
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
        sickLeaveCategory.setAllocatedDays(10);

        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(sickLeaveCategory);
        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId))).thenReturn(List.of());
        when(leaveRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        leaveService.applyLeave(request, userId);

        verify(annualLeaveService, never()).syncOnLeaveCreated(any(), any(), anyInt(), anyInt());
    }

    @Test
    void shouldThrowBadRequestWhenNonAnnualLeaveBalanceIsExhausted() {
        User user = createValidUser();
        LeaveCategory sickLeave = new LeaveCategory();
        sickLeave.setId(leaveCategoryId);
        sickLeave.setName("Sick Leave");
        sickLeave.setAllocatedDays(2);

        LocalDate weekday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));

        CreateLeaveRequest request = new CreateLeaveRequest();
        request.setLeaveCategoryId(leaveCategoryId);
        request.setDates(List.of(weekday));
        request.setDuration(DurationType.FULL_DAY);
        request.setStartTime(LocalTime.of(9, 0));
        request.setDescription("Feeling sick");

        lenient().when(userService.getUserByUserId(userId)).thenReturn(user);
        lenient().when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(sickLeave);

        lenient().when(holidayService.getHolidaysByType(HolidayType.FIXED)).thenReturn(new ArrayList<>());

        lenient().when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId), any(Sort.class)))
                .thenReturn(new ArrayList<>());
        lenient().when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId)))
                .thenReturn(new ArrayList<>());

        Leave takenLeave1 = new Leave();
        takenLeave1.setLeaveCategory(sickLeave);
        takenLeave1.setDuration(DurationType.FULL_DAY);

        Leave takenLeave2 = new Leave();
        takenLeave2.setLeaveCategory(sickLeave);
        takenLeave2.setDuration(DurationType.FULL_DAY);

        lenient().when(leaveRepository.findAllByUserIdAndDateBetweenAndDeletedAtIsNull(
                        eq(userId), any(LocalDate.class), any(LocalDate.class), any(Sort.class)))
                .thenReturn(List.of(takenLeave1, takenLeave2));

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.applyLeave(request, userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getMessage().contains("Insufficient leave balance for Sick Leave"));
    }

    @Test
    void shouldCalculateRequestedDaysCorrectlyForHalfDayAnnualLeave() {
        User user = createValidUser();
        LeaveCategory annualLeave = new LeaveCategory();
        annualLeave.setId(leaveCategoryId);
        annualLeave.setName(LeaveConstants.ANNUAL_LEAVE);

        LocalDate date = nextWeekday();
        CreateLeaveRequest request = new CreateLeaveRequest(
                leaveCategoryId, null, List.of(date), DurationType.HALF_DAY,
                LocalTime.of(9, 0), "Half day test");

        when(userService.getUserByUserId(userId)).thenReturn(user);
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(annualLeave);
        when(holidayService.getHolidaysByType(HolidayType.FIXED)).thenReturn(List.of());

        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId))).thenReturn(List.of());

        when(leaveRepository.findByUserIdAndDate(any(), any())).thenReturn(Optional.empty());
        when(leaveRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<CreateLeaveResponse> responses = leaveService.applyLeave(request, userId);

        assertEquals(1, responses.size());
        assertEquals(DurationType.HALF_DAY, responses.get(0).getDuration());

        verify(annualLeaveService).syncOnLeaveCreated(eq(user), eq(DurationType.HALF_DAY), eq(responses.size()), anyInt());
    }

    @Test
    void shouldThrowBadRequestWhenHalfDayIsAppliedToNonAnnualCategory() {
        LeaveCategory sickLeave = new LeaveCategory();
        sickLeave.setId(leaveCategoryId);
        sickLeave.setName("Sick Leave");
        sickLeave.setAllocatedDays(10);

        LocalDate weekday = nextWeekday();

        CreateLeaveRequest request = new CreateLeaveRequest(
                leaveCategoryId, null, List.of(weekday), DurationType.HALF_DAY,
                LocalTime.of(9, 0), "Should fail due to half day rule");

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(sickLeave);

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.applyLeave(request, userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Sick Leave can only be applied as a full day", ex.getMessage());
    }

    @Test
    void shouldHandleNullOldCategoryWhenUpdatingHolidayLeave() {
        User user = createValidUser();
        Holiday holiday = createOptionalHoliday();

        Leave holidayLeave = new Leave();
        holidayLeave.setId(UUID.randomUUID());
        holidayLeave.setUser(user);
        holidayLeave.setLeaveCategory(null);
        holidayLeave.setHoliday(holiday);
        holidayLeave.setDate(LocalDate.now());
        holidayLeave.setDuration(DurationType.FULL_DAY);
        holidayLeave.setStartTime(LocalTime.of(9, 0));
        holidayLeave.setDescription("Original Description");

        UpdateLeaveRequest request = new UpdateLeaveRequest();
        request.setDescription("Updated holiday description");

        when(leaveRepository.findById(holidayLeave.getId())).thenReturn(Optional.of(holidayLeave));
        when(leaveRepository.save(any(Leave.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateLeaveResponse response = leaveService.updateLeave(holidayLeave.getId(), request, userId);

        assertNotNull(response);
        assertEquals("Updated holiday description", response.getDescription());
        assertEquals(holiday.getType().getDisplayName(), response.getLeaveCategoryName());

        verify(annualLeaveService, never()).syncOnLeaveUpdated(any(), any(), any(), any(), any(), anyInt());
    }

    @Test
    void shouldSyncBalanceWhenUpdatingNonAnnualToAnnualLeave() {
        User user = createValidUser();
        LeaveCategory sickCat = new LeaveCategory(UUID.randomUUID(), "Sick Leave", 10, null, null);
        LeaveCategory annualCat = new LeaveCategory(UUID.randomUUID(), LeaveConstants.ANNUAL_LEAVE, 24, null, null);

        Leave existingLeave = createEmployeeLeave(user, sickCat);

        UpdateLeaveRequest request = new UpdateLeaveRequest();
        request.setLeaveCategoryId(annualCat.getId());

        when(leaveRepository.findById(existingLeave.getId())).thenReturn(Optional.of(existingLeave));
        when(leaveCategoryService.getLeaveCategoryById(annualCat.getId())).thenReturn(annualCat);
        when(leaveRepository.save(any(Leave.class))).thenAnswer(inv -> inv.getArgument(0));

        leaveService.updateLeave(existingLeave.getId(), request, userId);

        // This covers the second half of the || condition on line 424
        verify(annualLeaveService).syncOnLeaveUpdated(
                eq(user), eq("Sick Leave"), eq(LeaveConstants.ANNUAL_LEAVE), any(), any(), anyInt()
        );
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
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId)))
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
        when(leaveRepository.findAllByUserIdAndDeletedAtNull(eq(userId))).thenReturn(List.of());
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

        Page<Leave> leaves = new PageImpl<>(List.of(holidayLeave));

        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(leaveRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(leaves);

        Page<LeaveResponse> result = leaveService.getAllLeaves(
                employee.getId(), buildFilter("SELF", null, null, null), pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(holiday.getType().getDisplayName(), result.getContent().getFirst().type);
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

        LeaveCategory category = new LeaveCategory();
        category.setId(leaveCategoryId);
        category.setName(LeaveConstants.ANNUAL_LEAVE);
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(category);

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
        assertEquals("The selected date(s) fall on a weekend or fixed holiday. Please choose a working day.", ex.getMessage());
    }

    @Test
    void shouldExcludeLeaveIdWhenComputingTakenDays() {
        UUID categoryId = UUID.randomUUID();
        int year = LocalDate.now().getYear();

        LeaveCategory category = new LeaveCategory();
        category.setId(categoryId);

        UUID leaveToExcludeId = UUID.randomUUID();

        Leave leave1 = new Leave();
        leave1.setId(leaveToExcludeId);
        leave1.setLeaveCategory(category);
        leave1.setDuration(DurationType.FULL_DAY);

        Leave leave2 = new Leave();
        leave2.setId(UUID.randomUUID());
        leave2.setLeaveCategory(category);
        leave2.setDuration(DurationType.HALF_DAY);

        when(leaveRepository.findAllByUserIdAndDateBetweenAndDeletedAtIsNull(
                eq(userId), any(LocalDate.class), any(LocalDate.class), any(Sort.class)))
                .thenReturn(List.of(leave1, leave2));

        double totalDays = leaveService.computeTakenDays(userId, categoryId, year, leaveToExcludeId);

        assertEquals(0.5, totalDays);

        double totalWithNull = leaveService.computeTakenDays(userId, categoryId, year, null);
        assertEquals(1.5, totalWithNull);
    }

    @Test
    void shouldFilterDifferentCategoryWhenComputingTakenDays() {
        UUID targetCategoryId = UUID.randomUUID();
        LeaveCategory targetCategory = new LeaveCategory();
        targetCategory.setId(targetCategoryId);

        LeaveCategory otherCategory = new LeaveCategory();
        otherCategory.setId(UUID.randomUUID());

        Leave leaveTarget = new Leave();
        leaveTarget.setLeaveCategory(targetCategory);
        leaveTarget.setDuration(DurationType.FULL_DAY);

        Leave leaveOther = new Leave();
        leaveOther.setLeaveCategory(otherCategory);
        leaveOther.setDuration(DurationType.FULL_DAY);

        when(leaveRepository.findAllByUserIdAndDateBetweenAndDeletedAtIsNull(
                eq(userId), any(LocalDate.class), any(LocalDate.class), any(Sort.class)))
                .thenReturn(List.of(leaveTarget, leaveOther));

        double totalDays = leaveService.computeTakenDays(userId, targetCategoryId, LocalDate.now().getYear(), null);

        assertEquals(1.0, totalDays);
    }

    @Test
    void shouldThrowBadRequestWhenNewLeaveDateIsInPreviousMonth() {
        User user = createValidUser();
        LeaveCategory category = new LeaveCategory();
        category.setId(leaveCategoryId);
        category.setName(LeaveConstants.ANNUAL_LEAVE);

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setDate(LocalDate.now().plusDays(3));
        leave.setLeaveCategory(category);
        leave.setDuration(DurationType.FULL_DAY);

        UpdateLeaveRequest request = new UpdateLeaveRequest();
        request.setDate(LocalDate.now().minusMonths(1));

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.updateLeave(leave.getId(), request, userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Past dates can only be selected within the current month.", ex.getMessage());
    }

    @Test
    void shouldNotThrowWhenNewLeaveDateIsInCurrentMonth() {
        LeaveCategory category = new LeaveCategory();
        category.setId(leaveCategoryId);
        category.setName(LeaveConstants.ANNUAL_LEAVE);

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(createValidUser());
        leave.setDate(LocalDate.now().plusDays(3));
        leave.setLeaveCategory(category);
        leave.setDuration(DurationType.FULL_DAY);

        UpdateLeaveRequest request = new UpdateLeaveRequest();
        request.setDate(LocalDate.now().withDayOfMonth(1));

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
        when(leaveRepository.existsByUserIdAndDateAndIdNotAndDeletedAtIsNull(any(), any(), any())).thenReturn(false);
        when(leaveRepository.save(any(Leave.class))).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> leaveService.updateLeave(leave.getId(), request, userId));
    }

    @Test
    void shouldTriggerIntegrationHandlerUpdateWhenLeaveIsSuccessfullyUpdated() {
        User user = createValidUser();
        LeaveCategory category = createValidLeaveCategory();
        Leave leave = createEmployeeLeave(user, category);

        leave.setDate(nextWeekday());

        when(leaveRepository.findById(leave.getId())).thenReturn(Optional.of(leave));
        when(leaveCategoryService.getLeaveCategoryById(any())).thenReturn(category);
        when(leaveRepository.save(any(Leave.class))).thenReturn(leave);
        when(leaveRepository.existsByUserIdAndDateAndIdNotAndDeletedAtIsNull(any(), any(), any()))
                .thenReturn(false);

        UpdateLeaveRequest request = createValidUpdateRequest();
        request.setDate(nextWeekdays(5).get(4));
        leaveService.updateLeave(leave.getId(), request, userId);

        verify(leaveIntegrationHandler, times(1)).handleLeaveUpdate(any(Leave.class));
    }

    @Test
    void shouldUseValidateNewRequestLeaveDateWhenTypeIsRequest() {
        User user = createValidUser();
        LeaveCategory category = createValidLeaveCategory();

        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(user);
        leave.setDate(nextWeekday());
        leave.setDuration(DurationType.FULL_DAY);
        leave.setLeaveCategory(category);

        LocalDate validDate = LocalDate.now().minusDays(5);
        while (leaveService.isWeekendDay(validDate)) {
            validDate = validDate.minusDays(3);
        }

        UpdateLeaveRequest request = new UpdateLeaveRequest();
        request.setDate(validDate);
        request.setType("request");

        when(leaveRepository.findById(leave.getId()))
                .thenReturn(Optional.of(leave));
        when(leaveRepository.existsByUserIdAndDateAndIdNotAndDeletedAtIsNull(any(), any(), any()))
                .thenReturn(false);
        when(leaveRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() ->
                leaveService.updateLeave(leave.getId(), request, userId));
    }

    @Test
    void shouldThrowBadRequestWhenDateIsFromDifferentYear() {
        LocalDate invalidDate = LocalDate.now().minusYears(1);

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.validateNewRequestLeaveDate(invalidDate));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Date must be within the current year", ex.getMessage());
    }

    @Test
    void shouldThrowBadRequestWhenDateIsOlderThan30Days() {
        LocalDate invalidDate = LocalDate.now().minusDays(31);

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.validateNewRequestLeaveDate(invalidDate));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Date must be within the last 30 days excluding today", ex.getMessage());
    }

    @Test
    void shouldThrowBadRequestWhenDateIsInFuture() {
        LocalDate invalidDate = LocalDate.now().plusDays(1);

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.validateNewRequestLeaveDate(invalidDate));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Date must be within the last 30 days excluding today", ex.getMessage());
    }

    @Test
    void shouldThrowBadRequestWhenDateIsToday() {
        LocalDate today = LocalDate.now();

        HttpException ex = assertThrows(HttpException.class,
                () -> leaveService.validateNewRequestLeaveDate(today));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void shouldNotThrowWhenDateIsWithinLast30DaysAndNotToday() {
        LocalDate validDate = LocalDate.now().minusDays(5);

        assertDoesNotThrow(() ->
                leaveService.validateNewRequestLeaveDate(validDate));
    }
}
