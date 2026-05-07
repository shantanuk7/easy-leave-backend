package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.LeaveIntegrationEvent;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.IntegrationOperationType;
import com.technogise.leave_management_system.enums.IntegrationStatus;
import com.technogise.leave_management_system.enums.PlatformType;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.LeaveIntegrationEventRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class GoogleCalendarServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private LeaveIntegrationEventRepository leaveIntegrationEventRepository;

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    @InjectMocks
    private GoogleCalendarService googleCalendarService;

    private User user;
    private Leave leave;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setName("Priyansh");
        user.setEmail("priyansh@technogise.com");
        user.setGoogleAccessToken("valid-token");
        user.setGoogleTokenExpiry(LocalDateTime.now().plusHours(1));

        LeaveCategory category = new LeaveCategory();
        category.setName("Annual Leave");

        leave = new Leave();
        leave.setDate(LocalDate.now());
        leave.setUser(user);
        leave.setLeaveCategory(category);
        leave.setDescription("Personal Work");

        ReflectionTestUtils.setField(googleCalendarService, "calendarId", "test-calendar");
        ReflectionTestUtils.setField(googleCalendarService, "clientId", "client-id");
        ReflectionTestUtils.setField(googleCalendarService, "clientSecret", "client-secret");
        ReflectionTestUtils.setField(googleCalendarService, "httpClient", httpClient);
        ReflectionTestUtils.setField(googleCalendarService, "calendarApiBase", "https://www.googleapis.com/calendar/v3/calendars/");
        ReflectionTestUtils.setField(googleCalendarService, "timezone", "Asia/Kolkata");
    }

    @Test
    void shouldUseExistingTokenWhenNotExpired() throws Exception {
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("{\"id\":\"event-123\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        googleCalendarService.addLeaveEvent(user, leave, "title", "desc");
    }

    @Test
    void shouldRefreshTokenWhenExpired() throws Exception {
        user.setGoogleTokenExpiry(LocalDateTime.now().minusMinutes(10));
        user.setGoogleRefreshToken("refresh-token");

        when(httpResponse.body())
                .thenReturn("{\"access_token\":\"new-token\",\"expires_in\":3599}")
                .thenReturn("{\"id\":\"event-123\"}");
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        googleCalendarService.addLeaveEvent(user, leave, "title", "desc");
        verify(userRepository).save(user);
        assertEquals("new-token", user.getGoogleAccessToken());
    }

    @Test
    void shouldThrowWhenRefreshTokenMissing() {
        user.setGoogleTokenExpiry(LocalDateTime.now().minusMinutes(10));
        user.setGoogleRefreshToken(null);

        HttpException ex = assertThrows(HttpException.class,
                () -> googleCalendarService.addLeaveEvent(user, leave, "title", "desc"));

        assertTrue(ex.getMessage().contains("Failed to add leave"));
    }

    @Test
    void shouldSaveIntegrationEventOnSuccess() throws Exception {
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("{\"id\":\"event-xyz\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        googleCalendarService.addLeaveEvent(user, leave, "title", "desc");

        verify(leaveIntegrationEventRepository).save(argThat(event ->
                event.getExternalEventId().equals("event-xyz")
                        && event.getPlatform().equals(PlatformType.GOOGLE_CALENDAR)
        ));
    }

    @Test
    void shouldRefreshTokenWhenExpiryIsNull() {
        user.setGoogleTokenExpiry(null);
        user.setGoogleRefreshToken(null);

        HttpException ex = assertThrows(HttpException.class,
                () -> googleCalendarService.addLeaveEvent(user, leave, "title", "desc"));

        assertTrue(ex.getMessage().contains("Failed to add leave"));
    }

    @Test
    void shouldThrowWhenApiFails() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> googleCalendarService.addLeaveEvent(user, leave, "title", "desc"));

        assertTrue(ex.getMessage().contains("Failed to add leave"));
    }

    @Test
    void shouldSyncLeaveWithCorrectTitleAndDescription() throws Exception {
        when(httpResponse.statusCode()).thenReturn(201);
        when(httpResponse.body()).thenReturn("{\"id\":\"event-sync-1\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        googleCalendarService.syncLeave(leave);

        verify(leaveIntegrationEventRepository).save(argThat(event ->
                event.getExternalEventId().equals("event-sync-1")
                        && event.getPlatform().equals(PlatformType.GOOGLE_CALENDAR)
        ));
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(requestCaptor.capture(), any());
    }

    @Test
    void shouldSaveSuccessEventWhenGoogleCalendarDeleteSucceeds() throws Exception {
        LeaveIntegrationEvent existingEvent = new LeaveIntegrationEvent();
        existingEvent.setLeave(leave);
        existingEvent.setPlatform(PlatformType.GOOGLE_CALENDAR);
        existingEvent.setExternalEventId("event123");
        existingEvent.setStatus(IntegrationStatus.SUCCESS);

        when(leaveIntegrationEventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                leave.getId(), PlatformType.GOOGLE_CALENDAR, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.of(existingEvent));

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        when(httpResponse.statusCode()).thenReturn(204);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        googleCalendarService.deleteLeave(leave);

        verify(leaveIntegrationEventRepository).save(argThat(event ->
                event.getStatus() == IntegrationStatus.SUCCESS
                        && "event123".equals(event.getExternalEventId())
                        && event.getOperationType() == IntegrationOperationType.DELETE
        ));
    }

    @Test
    void shouldSaveSuccessEventWhenGoogleCalendarReturns404() throws Exception {
        LeaveIntegrationEvent existingEvent = new LeaveIntegrationEvent();
        existingEvent.setLeave(leave);
        existingEvent.setPlatform(PlatformType.GOOGLE_CALENDAR);
        existingEvent.setExternalEventId("event123");
        existingEvent.setStatus(IntegrationStatus.SUCCESS);

        when(leaveIntegrationEventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                leave.getId(), PlatformType.GOOGLE_CALENDAR, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        when(httpResponse.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        googleCalendarService.deleteLeave(leave);

        verify(leaveIntegrationEventRepository).save(argThat(event ->
                event.getStatus() == IntegrationStatus.SUCCESS
                        && event.getOperationType() == IntegrationOperationType.DELETE));
    }

    @Test
    void shouldSaveSuccessEventWhenGoogleCalendarReturns200() throws Exception {
        LeaveIntegrationEvent existingEvent = new LeaveIntegrationEvent();
        existingEvent.setLeave(leave);
        existingEvent.setPlatform(PlatformType.GOOGLE_CALENDAR);
        existingEvent.setExternalEventId("event123");
        existingEvent.setStatus(IntegrationStatus.SUCCESS);

        when(leaveIntegrationEventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                leave.getId(), PlatformType.GOOGLE_CALENDAR, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.of(existingEvent));

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        googleCalendarService.deleteLeave(leave);

        verify(leaveIntegrationEventRepository).save(argThat(event ->
                event.getStatus() == IntegrationStatus.SUCCESS
                        && "event123".equals(event.getExternalEventId())
                        && event.getOperationType() == IntegrationOperationType.DELETE));
    }

    @Test
    void shouldSaveFailedEventWhenUserNotFoundDuringDelete() {
        LeaveIntegrationEvent existingEvent = new LeaveIntegrationEvent();
        existingEvent.setExternalEventId("event123");
        existingEvent.setStatus(IntegrationStatus.SUCCESS);

        when(leaveIntegrationEventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                leave.getId(), PlatformType.GOOGLE_CALENDAR, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.of(existingEvent));

        when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

        googleCalendarService.deleteLeave(leave);

        verify(leaveIntegrationEventRepository).save(argThat(event ->
                event.getStatus() == IntegrationStatus.FAILED
                        && event.getErrorMessage().equals("User not found for leaveId=" + leave.getId())
                        && event.getOperationType() == IntegrationOperationType.DELETE
        ));
    }

    @Test
    void shouldSaveFailedEventWhenExternalEventIdIsNull() throws Exception {
        LeaveIntegrationEvent eventWithNullId = new LeaveIntegrationEvent();
        eventWithNullId.setExternalEventId(null);
        eventWithNullId.setStatus(IntegrationStatus.SUCCESS);

        when(leaveIntegrationEventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                leave.getId(), PlatformType.GOOGLE_CALENDAR, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.of(eventWithNullId));

        googleCalendarService.deleteLeave(leave);

        verify(leaveIntegrationEventRepository).save(argThat(event ->
                event.getStatus() == IntegrationStatus.FAILED
                        && event.getErrorMessage().contains("No Google Calendar entry found")
                        && event.getOperationType() == IntegrationOperationType.DELETE
        ));

        verify(userRepository, never()).findById(any());
        verify(httpClient, never()).send(any(), any());
    }

    @Test
    void shouldSaveFailedEventWhenGoogleCalendarApiReturnsError() throws Exception {
        LeaveIntegrationEvent existingEvent = new LeaveIntegrationEvent();
        existingEvent.setLeave(leave);
        existingEvent.setPlatform(PlatformType.GOOGLE_CALENDAR);
        existingEvent.setExternalEventId("event123");
        existingEvent.setStatus(IntegrationStatus.SUCCESS);

        when(leaveIntegrationEventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                leave.getId(), PlatformType.GOOGLE_CALENDAR, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(httpResponse);

        googleCalendarService.deleteLeave(leave);

        verify(leaveIntegrationEventRepository).save(argThat(event ->
                event.getStatus() == IntegrationStatus.FAILED
                        && event.getErrorMessage().contains("500")
                        && event.getOperationType() == IntegrationOperationType.DELETE
        ));
    }

    @Test
    void shouldSaveFailedEventWhenExceptionOccurs() throws Exception {
        LeaveIntegrationEvent existingEvent = new LeaveIntegrationEvent();
        existingEvent.setLeave(leave);
        existingEvent.setPlatform(PlatformType.GOOGLE_CALENDAR);
        existingEvent.setExternalEventId("event123");
        existingEvent.setStatus(IntegrationStatus.SUCCESS);

        when(leaveIntegrationEventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                leave.getId(), PlatformType.GOOGLE_CALENDAR, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.of(existingEvent));

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        googleCalendarService.deleteLeave(leave);

        verify(leaveIntegrationEventRepository).save(argThat(event ->
                event.getStatus() == IntegrationStatus.FAILED
                        && "Connection refused".equals(event.getErrorMessage())
                        && event.getOperationType() == IntegrationOperationType.DELETE));
    }

    @Test
    void shouldQueryRepositoryWithCorrectLeaveIdAndPlatformOnDelete() {
        when(leaveIntegrationEventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                leave.getId(), PlatformType.GOOGLE_CALENDAR, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.empty());

        googleCalendarService.deleteLeave(leave);

        verify(leaveIntegrationEventRepository).findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                leave.getId(), PlatformType.GOOGLE_CALENDAR, IntegrationStatus.SUCCESS);
    }

    @Test
    void shouldUpdateSyncEventSuccessfully() throws Exception {
        LeaveIntegrationEvent existingEvent = new LeaveIntegrationEvent();
        existingEvent.setExternalEventId("event-existing-123");

        when(leaveIntegrationEventRepository
                .findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                        leave.getId(), PlatformType.GOOGLE_CALENDAR, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.of(existingEvent));

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        googleCalendarService.updateLeave(leave);

        verify(leaveIntegrationEventRepository).save(argThat(event ->
                event.getExternalEventId().equals("event-existing-123")
                        && event.getPlatform().equals(PlatformType.GOOGLE_CALENDAR)
                        && event.getOperationType().equals(IntegrationOperationType.UPDATE)
        ));
    }

    @Test
    void shouldThrowWhenGoogleCalendarApiFailsDuringUpdate() throws Exception {
        LeaveIntegrationEvent existingEvent = new LeaveIntegrationEvent();
        existingEvent.setExternalEventId("event-existing-123");

        when(leaveIntegrationEventRepository
                .findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                        leave.getId(), PlatformType.GOOGLE_CALENDAR, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.of(existingEvent));

        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));

        assertThrows(RuntimeException.class, () -> googleCalendarService.updateLeave(leave));
    }

    @Test
    void shouldSetEventAsFailedWhenNoExistingEventFound() {
        when(userRepository.findById(user.getId()))
                .thenReturn(Optional.of(user));
        when(leaveIntegrationEventRepository
                .findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                        leave.getId(), PlatformType.GOOGLE_CALENDAR, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.empty());

        googleCalendarService.updateLeave(leave);

        verify(leaveIntegrationEventRepository).save(argThat(event ->
                event.getStatus().equals(IntegrationStatus.FAILED)
                        && event.getErrorMessage().contains(String.valueOf(leave.getId()))
        ));
    }
}
