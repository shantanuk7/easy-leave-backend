package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.*;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.Request;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.RequestStatus;
import com.technogise.leave_management_system.enums.RequestType;
import com.technogise.leave_management_system.enums.ScopeType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.LeaveRepository;
import com.technogise.leave_management_system.repository.RequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyList;

@ExtendWith(MockitoExtension.class)
public class RequestServiceTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private LeaveRepository leaveRepository;

    @Mock
    private UserService userService;

    @Mock
    private LeaveCategoryService leaveCategoryService;

    @InjectMocks
    private RequestService requestService;

    @Mock
    private LeaveService leaveService;

    private User employee;
    private User manager;
    private Pageable pageable;
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private UUID userId;
    private UUID leaveCategoryId;

    @BeforeEach
    void setUp() {
        employee = new User();
        employee.setId(UUID.randomUUID());
        employee.setName("Priyansh");
        employee.setRole(UserRole.EMPLOYEE);

        manager = new User();
        manager.setId(UUID.randomUUID());
        manager.setName("Priyansh");
        manager.setRole(UserRole.MANAGER);
        userId = UUID.randomUUID();
        leaveCategoryId = UUID.randomUUID();
        pageable = PageRequest.of(0, 10);
    }

    private User createValidUser() {
        User user = new User();
        user.setId(userId);
        return user;
    }

    private CreateRequestPayload createPastLeavePayload(LocalDate date) {
        return createPastLeavePayload(List.of(date));
    }

    private CreateRequestPayload createPastLeavePayload(List<LocalDate> dates) {
        CreateRequestPayload payload = new CreateRequestPayload();
        payload.setRequestType(RequestType.PAST_LEAVE);
        payload.setLeaveCategoryId(leaveCategoryId);
        payload.setDates(dates);
        payload.setStartTime(LocalTime.of(9, 0));
        payload.setDuration(DurationType.FULL_DAY);
        payload.setDescription("Was sick");
        return payload;
    }

    private CreateRequestPayload createCompOffPayload(List<LocalDate> dates) {
        CreateRequestPayload payload = new CreateRequestPayload();
        payload.setRequestType(RequestType.COMPENSATORY_OFF);
        payload.setLeaveCategoryId(null);
        payload.setDates(dates);
        payload.setStartTime(LocalTime.of(10, 0));
        payload.setDuration(DurationType.FULL_DAY);
        payload.setDescription("Worked on Saturday for release");
        return payload;
    }

    private LocalDate lastWeekendDay() {
        LocalDate date = LocalDate.now(IST).minusDays(1);
        while (date.getDayOfWeek() != DayOfWeek.SATURDAY
                && date.getDayOfWeek() != DayOfWeek.SUNDAY) {
            date = date.minusDays(1);
        }
        return date;
    }

    private User createManager() {
        User manager = new User();
        manager.setId(UUID.randomUUID());
        manager.setName("Manager");
        manager.setRole(UserRole.MANAGER);
        return manager;
    }

    private LeaveCategory createCategory() {
        LeaveCategory category = new LeaveCategory();
        category.setId(UUID.randomUUID());
        category.setName("Annual Leave");
        return category;
    }

    private Request createRequest(User employee, LeaveCategory category) {
        Request request = new Request();
        request.setId(UUID.randomUUID());
        request.setRequestedByUser(employee);
        request.setDate(LocalDate.now().minusDays(5));
        request.setDuration(DurationType.FULL_DAY);
        request.setRequestType(RequestType.PAST_LEAVE);
        request.setStatus(RequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        request.setLeaveCategory(category);
        return request;
    }

    private Leave createLeave(User employee, LocalDate date) {
        Leave leave = new Leave();
        leave.setId(UUID.randomUUID());
        leave.setUser(employee);
        leave.setDate(date);
        return leave;
    }

    @Test
    void shouldThrowBadRequestWhenPastLeaveRequestHasNullLeaveCategoryId() {
        CreateRequestPayload payload = createPastLeavePayload(LocalDate.now(IST).minusDays(3));
        payload.setLeaveCategoryId(null);

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());

        HttpException ex = assertThrows(HttpException.class,
                () -> requestService.raiseRequest(payload, userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Leave category is required for Past Leave requests", ex.getMessage());
    }

    @Test
    void shouldThrowBadRequestWhenPastLeaveRequestHasDateOfToday() {
        CreateRequestPayload payload = createPastLeavePayload(LocalDate.now(IST));

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId))
                .thenReturn(new LeaveCategory());

        HttpException ex = assertThrows(HttpException.class,
                () -> requestService.raiseRequest(payload, userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Past leave dates must be within the last 30 days", ex.getMessage());
    }

    @Test
    void shouldThrowBadRequestWhenPastLeaveRequestHasDateInFuture() {
        CreateRequestPayload payload = createPastLeavePayload(LocalDate.now(IST).plusDays(1));

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId))
                .thenReturn(new LeaveCategory());

        HttpException ex = assertThrows(HttpException.class,
                () -> requestService.raiseRequest(payload, userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Past leave dates must be within the last 30 days", ex.getMessage());
    }

    @Test
    void shouldThrowBadRequestWhenPastLeaveRequestHasDateOlderThan30Days() {
        CreateRequestPayload payload = createPastLeavePayload(LocalDate.now(IST).minusDays(31));

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId))
                .thenReturn(new LeaveCategory());

        HttpException ex = assertThrows(HttpException.class,
                () -> requestService.raiseRequest(payload, userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Past leave dates must be within the last 30 days", ex.getMessage());
    }

    @Test
    void shouldAcceptDateExactly30DaysAgo() {
        LocalDate thirtyDaysAgo = LocalDate.now(IST).minusDays(30);

        LocalDate validDate = thirtyDaysAgo;
        while (validDate.getDayOfWeek() == DayOfWeek.SATURDAY
                || validDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            validDate = validDate.plusDays(1);
            if (!validDate.isBefore(LocalDate.now(IST))) {
                return;
            }
        }

        LeaveCategory leaveCategory = new LeaveCategory();
        leaveCategory.setId(leaveCategoryId);
        leaveCategory.setName("Sick Leave");

        CreateRequestPayload payload = createPastLeavePayload(validDate);

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(leaveCategory);

        when(requestRepository.existsByRequestedByUserIdAndDateInAndStatusIn(
                userId, List.of(validDate), List.of(RequestStatus.PENDING, RequestStatus.APPROVED)))
                .thenReturn(false);
        when(requestRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> requestService.raiseRequest(payload, userId));
    }

    @Test
    void shouldAcceptDateYesterday() {
        LocalDate yesterday = LocalDate.now(IST).minusDays(1);

        if (yesterday.getDayOfWeek() == DayOfWeek.SATURDAY
                || yesterday.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return;
        }

        LeaveCategory leaveCategory = new LeaveCategory();
        leaveCategory.setId(leaveCategoryId);
        leaveCategory.setName("Sick Leave");

        CreateRequestPayload payload = createPastLeavePayload(yesterday);

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(leaveCategory);

        when(requestRepository.existsByRequestedByUserIdAndDateInAndStatusIn(
                userId, List.of(yesterday), List.of(RequestStatus.PENDING, RequestStatus.APPROVED)))
                .thenReturn(false);
        when(requestRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> requestService.raiseRequest(payload, userId));
    }

    @Test
    void shouldThrowBadRequestWhenAllValidPastLeaveDatesAreWeekends() {
        LocalDate saturday = LocalDate.now(IST).minusDays(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY));

        CreateRequestPayload payload = createPastLeavePayload(saturday);

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId))
                .thenReturn(new LeaveCategory());

        HttpException ex = assertThrows(HttpException.class,
                () -> requestService.raiseRequest(payload, userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Cannot raise the request for weekend leave.", ex.getMessage());
    }

    private Request buildRequest(User user, LeaveCategory leaveCategory) {
        Request request = new Request();
        request.setId(UUID.randomUUID());
        request.setRequestedByUser(user);
        request.setRequestType(RequestType.PAST_LEAVE);
        request.setLeaveCategory(leaveCategory);
        request.setDate(LocalDate.now());
        request.setDuration(DurationType.FULL_DAY);
        request.setDescription("Sick");
        request.setStatus(RequestStatus.PENDING);
        request.setCreatedAt(LocalDateTime.now());
        return request;
    }

    @Test
    void shouldReturnRequestsForSelfScopeWithoutStatusFilter() {
        Request request = buildRequest(employee, null);
        Page<Request> mockPage = new PageImpl<>(List.of(request));

        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(requestRepository.findAllByRequestedByUserId(eq(employee.getId()), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<RequestResponse> result = requestService.getAllRequests(pageable, employee.getId(), ScopeType.SELF, null);
        assertEquals(1, result.getContent().size());
        assertEquals("Priyansh", result.getContent().getFirst().getEmployeeName());
        assertEquals("Sick", result.getContent().getFirst().getDescription());
    }
    @Test
    void shouldThrowConflictWhenPastLeaveRequestAlreadyExistsWithPendingStatus() {
        LocalDate lastWeekMonday = LocalDate.now(IST)
                .minusDays(7)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        CreateRequestPayload payload = createPastLeavePayload(lastWeekMonday);

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId))
                .thenReturn(new LeaveCategory());

        when(requestRepository.existsByRequestedByUserIdAndDateInAndStatusIn(
                userId, List.of(lastWeekMonday), List.of(RequestStatus.PENDING, RequestStatus.APPROVED)))
                .thenReturn(true);

        HttpException ex = assertThrows(HttpException.class,
                () -> requestService.raiseRequest(payload, userId));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("A request already exists for one or more of the selected dates. Please choose different dates.", ex.getMessage());
    }

    @Test
    void shouldThrowConflictWhenPastLeaveRequestAlreadyExistsWithApprovedStatus() {
        LocalDate lastWeekTuesday = LocalDate.now(IST)
                .minusDays(7)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));

        CreateRequestPayload payload = createPastLeavePayload(lastWeekTuesday);

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId))
                .thenReturn(new LeaveCategory());

        when(requestRepository.existsByRequestedByUserIdAndDateInAndStatusIn(
                userId, List.of(lastWeekTuesday), List.of(RequestStatus.PENDING, RequestStatus.APPROVED)))
                .thenReturn(true);

        HttpException ex = assertThrows(HttpException.class,
                () -> requestService.raiseRequest(payload, userId));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("A request already exists for one or more of the selected dates. Please choose different dates.", ex.getMessage());
    }

    @Test
    void shouldReturnRequestsForSelfScopeWithStatusFilter() {
        LeaveCategory leaveCategory = new LeaveCategory();
        leaveCategory.setName("Sick Leave");

        Request request = buildRequest(employee, leaveCategory);
        Page<Request> mockPage = new PageImpl<>(List.of(request));

        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);
        when(requestRepository.findAllByRequestedByUserIdAndStatus(
                eq(employee.getId()), eq(RequestStatus.PENDING), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<RequestResponse> result = requestService.getAllRequests(
                pageable, employee.getId(), ScopeType.SELF, RequestStatus.PENDING);

        assertEquals(1, result.getContent().size());
        assertEquals("Priyansh", result.getContent().getFirst().getEmployeeName());
        assertEquals("Sick Leave", result.getContent().getFirst().getLeaveCategory());
    }

    @Test
    void shouldReturnRequestsForOrganizationScope() {
        LeaveCategory leaveCategory = new LeaveCategory();
        leaveCategory.setName("Sick Leave");

        Request request = buildRequest(manager, leaveCategory);
        Page<Request> mockPage = new PageImpl<>(List.of(request));

        when(userService.getUserByUserId(manager.getId())).thenReturn(manager);
        when(requestRepository.findAllByStatus(
                eq(RequestStatus.PENDING), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<RequestResponse> result = requestService.getAllRequests(
                pageable, manager.getId(), ScopeType.ORGANIZATION, RequestStatus.PENDING);

        assertEquals(1, result.getContent().size());
        assertEquals("Priyansh", result.getContent().getFirst().getEmployeeName());
        assertEquals("Sick Leave", result.getContent().getFirst().getLeaveCategory());
    }

    @Test
    void shouldThrowErrorWhenEmployeeRequestWithScopeOrganization() {
        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);

        HttpException ex = assertThrows(HttpException.class, () ->
                requestService.getAllRequests(
                        pageable, employee.getId(), ScopeType.ORGANIZATION, RequestStatus.PENDING));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("Not Allowed to access this resource", ex.getMessage());
    }

    @Test
    void shouldThrowErrorWhenManagerRequestWithScopeOrganizationAndStatusApproved() {
        when(userService.getUserByUserId(manager.getId())).thenReturn(manager);

        HttpException ex = assertThrows(HttpException.class, () ->
                requestService.getAllRequests(
                        pageable, manager.getId(), ScopeType.ORGANIZATION, RequestStatus.APPROVED));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Managers can only access pending requests", ex.getMessage());
    }
    @Test
    void shouldReturnPendingRequestsWhenStatusIsNullForOrganizationScope() {
        LeaveCategory leaveCategory = new LeaveCategory();
        leaveCategory.setName("Sick Leave");

        Request request = buildRequest(manager, leaveCategory);
        Page<Request> mockPage = new PageImpl<>(List.of(request));

        when(userService.getUserByUserId(manager.getId())).thenReturn(manager);
        when(requestRepository.findAllByStatus(eq(RequestStatus.PENDING), any(Pageable.class))).thenReturn(mockPage);

        Page<RequestResponse> result = requestService.getAllRequests(pageable, manager.getId(), ScopeType.ORGANIZATION, null);

        assertEquals(1, result.getContent().size());
        verify(requestRepository).findAllByStatus(
                eq(RequestStatus.PENDING),
                any(Pageable.class)
        );
    }
    @Test
    void shouldNotThrowConflictWhenPastLeaveRequestExistsWithRejectedStatus() {
        LocalDate lastWeekMonday = LocalDate.now(IST)
                .minusDays(7)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        LeaveCategory leaveCategory = new LeaveCategory();
        leaveCategory.setId(leaveCategoryId);
        leaveCategory.setName("Sick Leave");

        CreateRequestPayload payload = createPastLeavePayload(lastWeekMonday);

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(leaveCategory);

        when(requestRepository.existsByRequestedByUserIdAndDateInAndStatusIn(
                userId, List.of(lastWeekMonday), List.of(RequestStatus.PENDING, RequestStatus.APPROVED)))
                .thenReturn(false);
        when(requestRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> requestService.raiseRequest(payload, userId));
    }

    @Test
    void shouldThrowBadRequestForInvalidScope() {
        when(userService.getUserByUserId(employee.getId())).thenReturn(employee);

        HttpException exception = assertThrows(HttpException.class, () ->
                requestService.getAllRequests(
                        pageable, employee.getId(), null, null));

        assertTrue(exception.getMessage().contains("Invalid scope"));
    }

    @Test
    void shouldSaveOneRequestPerValidDateAndReturnResponsesForPastLeave() {

        LocalDate lastWeekMonday = LocalDate.now(IST)
                .minusDays(7)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate lastWeekTuesday = lastWeekMonday.plusDays(1);

        CreateRequestPayload payload = createPastLeavePayload(List.of(lastWeekMonday, lastWeekTuesday));

        LeaveCategory leaveCategory = new LeaveCategory();
        leaveCategory.setId(leaveCategoryId);
        leaveCategory.setName("Sick Leave");

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(leaveCategory);

        when(requestRepository.existsByRequestedByUserIdAndDateInAndStatusIn(
                userId, List.of(lastWeekMonday, lastWeekTuesday),
                List.of(RequestStatus.PENDING, RequestStatus.APPROVED)))
                .thenReturn(false);
        when(requestRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CreateRequestResponse> responses = requestService.raiseRequest(payload, userId);

        assertEquals(2, responses.size());
        assertEquals(lastWeekMonday, responses.get(0).getDate());
        assertEquals(lastWeekTuesday, responses.get(1).getDate());
        assertEquals(RequestType.PAST_LEAVE, responses.get(0).getRequestType());
        assertEquals(RequestStatus.PENDING, responses.get(0).getStatus());
        assertEquals("Sick Leave", responses.get(0).getLeaveCategoryName());
    }

    @Test
    void shouldSkipWeekendDatesAndSaveOnlyWorkingDaysForPastLeave() {
        LocalDate lastWeekMonday = LocalDate.now(IST)
                .minusDays(7)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        LocalDate lastWeekSaturday = lastWeekMonday
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        CreateRequestPayload payload = createPastLeavePayload(
                List.of(lastWeekMonday, lastWeekSaturday));

        LeaveCategory leaveCategory = new LeaveCategory();
        leaveCategory.setId(leaveCategoryId);
        leaveCategory.setName("Sick Leave");

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(leaveCategory);

        when(requestRepository.existsByRequestedByUserIdAndDateInAndStatusIn(
                userId, List.of(lastWeekMonday),
                List.of(RequestStatus.PENDING, RequestStatus.APPROVED)))
                .thenReturn(false);
        when(requestRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CreateRequestResponse> responses = requestService.raiseRequest(payload, userId);

        assertEquals(1, responses.size());
        assertEquals(lastWeekMonday, responses.get(0).getDate());
    }

    @Test
    void shouldSaveRequestWithPendingStatusByDefault() {
        LocalDate lastWeekMonday = LocalDate.now(IST)
                .minusDays(7)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        LeaveCategory leaveCategory = new LeaveCategory();
        leaveCategory.setId(leaveCategoryId);
        leaveCategory.setName("Sick Leave");

        CreateRequestPayload payload = createPastLeavePayload(lastWeekMonday);

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());
        when(leaveCategoryService.getLeaveCategoryById(leaveCategoryId)).thenReturn(leaveCategory);

        when(requestRepository.existsByRequestedByUserIdAndDateInAndStatusIn(
                userId, List.of(lastWeekMonday),
                List.of(RequestStatus.PENDING, RequestStatus.APPROVED)))
                .thenReturn(false);
        when(requestRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CreateRequestResponse> responses = requestService.raiseRequest(payload, userId);

        assertEquals(1, responses.size());
        assertEquals(RequestStatus.PENDING, responses.get(0).getStatus());
    }

    @Test
    void shouldThrowBadRequestWhenCompOffDateIsOlderThan30Days() {
        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());

        HttpException ex = assertThrows(HttpException.class,
                () -> requestService.raiseRequest(
                        createCompOffPayload(List.of(LocalDate.now(IST).minusDays(31))), userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Compensatory off dates must be within the last 30 days", ex.getMessage());
    }

    @Test
    void shouldThrowBadRequestWhenCompOffDateIsTodayOrInFuture() {
        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());

        HttpException ex = assertThrows(HttpException.class,
                () -> requestService.raiseRequest(
                        createCompOffPayload(List.of(LocalDate.now(IST))), userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Compensatory off dates must be within the last 30 days", ex.getMessage());
    }

    @Test
    void shouldThrowConflictWhenCompOffRequestAlreadyExistsForDate() {
        LocalDate weekend = lastWeekendDay();

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());

        when(requestRepository.existsByRequestedByUserIdAndDateInAndStatusIn(
                userId, List.of(weekend), List.of(RequestStatus.PENDING, RequestStatus.APPROVED)))
                .thenReturn(true);

        HttpException ex = assertThrows(HttpException.class,
                () -> requestService.raiseRequest(createCompOffPayload(List.of(weekend)), userId));

        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("A request already exists for one or more of the selected dates. Please choose different dates.", ex.getMessage());
    }

    @Test
    void shouldSaveCompOffRequestWithNullLeaveCategoryAndPendingStatus() {
        LocalDate weekend = lastWeekendDay();

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());

        when(requestRepository.existsByRequestedByUserIdAndDateInAndStatusIn(
                userId, List.of(weekend), List.of(RequestStatus.PENDING, RequestStatus.APPROVED)))
                .thenReturn(false);
        when(requestRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        List<CreateRequestResponse> responses =
                requestService.raiseRequest(createCompOffPayload(List.of(weekend)), userId);

        assertEquals(1, responses.size());
        assertEquals(RequestType.COMPENSATORY_OFF, responses.get(0).getRequestType());
        assertEquals(RequestStatus.PENDING, responses.get(0).getStatus());
        assertNull(responses.get(0).getLeaveCategoryName());
        assertEquals(weekend, responses.get(0).getDate());
        verify(leaveCategoryService, never()).getLeaveCategoryById(any());
    }

    @Test
    void shouldThrowBadRequestWhenAllValidCompOffDatesAreWeekdays() {
        LocalDate weekday = LocalDate.now(IST)
                .minusDays(7)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));

        when(userService.getUserByUserId(userId)).thenReturn(createValidUser());

        HttpException ex = assertThrows(HttpException.class,
                () -> requestService.raiseRequest(createCompOffPayload(List.of(weekday)), userId));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Compensatory off dates must fall on a weekend (Saturday or Sunday)", ex.getMessage());
    }

    @Test
    void shouldApproveRequestAndUpdateLeaveAndReturnResponse() {
        User manager = createManager();
        User employee = createValidUser();
        employee.setName("Employee");
        LeaveCategory category = createCategory();

        Request request = createRequest(employee, category);
        Leave leave = createLeave(employee, request.getDate());

        UpdateRequestPayload payload = new UpdateRequestPayload();
        payload.setStatus(RequestStatus.APPROVED);
        payload.setManagerRemark("Approved");

        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(leaveRepository.findByUserIdAndDate(employee.getId(), request.getDate()))
                .thenReturn(Optional.of(leave));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RequestResponse response =
                requestService.handleRequest(manager, request.getId(), payload);

        assertEquals(RequestStatus.APPROVED, response.getStatus());
        assertEquals("Approved", response.getManagerRemark());
        assertEquals("Employee", response.getEmployeeName());

        verify(leaveService).updateLeave(eq(leave.getId()), any(), eq(employee.getId()));
        verify(requestRepository).save(request);
    }

    @Test
    void shouldThrowNotFoundWhenRequestDoesNotExist() {
        UUID requestId = UUID.randomUUID();

        when(requestRepository.findById(requestId)).thenReturn(Optional.empty());

        HttpException ex = assertThrows(HttpException.class,
                () -> requestService.handleRequest(new User(), requestId, new UpdateRequestPayload()));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void shouldHandleRejectedRequestWithoutUpdatingLeave() {
        User manager = createManager();
        User employee = createValidUser();
        employee.setName("Employee");
        LeaveCategory category = createCategory();

        Request request = createRequest(employee, category);

        UpdateRequestPayload payload = new UpdateRequestPayload();
        payload.setStatus(RequestStatus.REJECTED);
        payload.setManagerRemark("Not valid");

        when(requestRepository.findById(request.getId()))
                .thenReturn(Optional.of(request));
        when(requestRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
        RequestResponse response =
                requestService.handleRequest(manager, request.getId(), payload);

        assertEquals(RequestStatus.REJECTED, response.getStatus());
        assertEquals("Not valid", response.getManagerRemark());
        assertEquals(employee.getName(), response.getEmployeeName());

        verify(leaveRepository, never()).findByUserIdAndDate(any(), any());
        verify(leaveService, never()).updateLeave(any(), any(), any());
        verify(requestRepository).save(request);
    }

    @Test
    void shouldThrowNotFoundWhenLeaveDoesNotExist() {
        User manager = new User();
        User employee = createValidUser();

        Request request = new Request();
        request.setId(UUID.randomUUID());
        request.setRequestedByUser(employee);
        request.setDate(LocalDate.now().minusDays(5));

        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(leaveRepository.findByUserIdAndDate(employee.getId(), request.getDate()))
                .thenReturn(Optional.empty());

        HttpException ex = assertThrows(HttpException.class,
                () -> requestService.handleRequest(manager, request.getId(), new UpdateRequestPayload()));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void shouldNotSetManagerRemarkWhenRemarkIsNull() {
        User manager = createManager();
        User employee = createValidUser();
        employee.setName("Employee");
        LeaveCategory category = createCategory();

        Request request = createRequest(employee, category);
        Leave leave = createLeave(employee, request.getDate());

        UpdateRequestPayload payload = new UpdateRequestPayload();
        payload.setStatus(RequestStatus.APPROVED);
        payload.setManagerRemark(null);

        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(leaveRepository.findByUserIdAndDate(employee.getId(), request.getDate()))
                .thenReturn(Optional.of(leave));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RequestResponse response =
                requestService.handleRequest(manager, request.getId(), payload);

        assertNull(response.getManagerRemark());
    }

    @Test
    void shouldNotSetManagerRemarkWhenRemarkIsBlank() {
        User manager = createManager();
        User employee = createValidUser();
        employee.setName("Employee");
        LeaveCategory category = createCategory();

        Request request = createRequest(employee, category);
        Leave leave = createLeave(employee, request.getDate());

        UpdateRequestPayload payload = new UpdateRequestPayload();
        payload.setStatus(RequestStatus.APPROVED);
        payload.setManagerRemark(" ");

        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(leaveRepository.findByUserIdAndDate(employee.getId(), request.getDate()))
                .thenReturn(Optional.of(leave));
        when(requestRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RequestResponse response =
                requestService.handleRequest(manager, request.getId(), payload);

        assertNull(response.getManagerRemark());
    }

    @Test
    void shouldReturnRejectedMessageWhenStatusIsRejected() {
        RequestResponse response = new RequestResponse();
        response.setStatus(RequestStatus.REJECTED);

        String result = requestService.getResponseMessage(response);

        assertEquals("Request rejected successfully", result);
    }

    @Test
    void shouldReturnApprovedMessageWhenStatusIsApproved() {
        RequestResponse response = new RequestResponse();
        response.setStatus(RequestStatus.APPROVED);

        String result = requestService.getResponseMessage(response);

        assertEquals("Request approved successfully", result);
    }
}
