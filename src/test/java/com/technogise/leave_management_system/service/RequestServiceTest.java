package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.RequestResponse;
import com.technogise.leave_management_system.dto.CreateRequestPayload;
import com.technogise.leave_management_system.dto.CreateRequestResponse;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

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

    private User employee;
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
        userId = UUID.randomUUID();
        leaveCategoryId = UUID.randomUUID();
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
        assertEquals("A request already exists for one of the selected dates", ex.getMessage());
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
        assertEquals("A request already exists for one of the selected dates", ex.getMessage());
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
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
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
}
