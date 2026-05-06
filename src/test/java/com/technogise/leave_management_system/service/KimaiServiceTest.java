package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.KimaiTimesheetResponse;
import com.technogise.leave_management_system.dto.KimaiUserResponse;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveIntegrationEvent;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.entity.Holiday;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.IntegrationStatus;
import com.technogise.leave_management_system.enums.PlatformType;
import com.technogise.leave_management_system.enums.UserRole;
import com.technogise.leave_management_system.enums.HolidayType;
import com.technogise.leave_management_system.enums.IntegrationOperationType;
import com.technogise.leave_management_system.repository.LeaveIntegrationEventRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KimaiServiceTest {
    @Mock
    private WebClient webClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec postHeadersSpec;

    @Mock
    private WebClient.RequestHeadersSpec getHeadersSpec;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Spy
    @InjectMocks
    private KimaiService kimaiService;

    @Mock
    private LeaveIntegrationEventRepository eventRepository;

    private Leave testLeave;
    private KimaiUserResponse mockKimaiUser;
    private KimaiTimesheetResponse response;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setName("Raj");
        user.setEmail("raj@technogise.com");
        user.setRole(UserRole.EMPLOYEE);

        LeaveCategory leaveCategory = new LeaveCategory();
        leaveCategory.setId(UUID.randomUUID());
        leaveCategory.setName("Annual Leave");

        testLeave = new Leave();
        testLeave.setId(UUID.randomUUID());
        testLeave.setUser(user);
        testLeave.setLeaveCategory(leaveCategory);
        testLeave.setDate(LocalDate.of(2026, 4, 26));
        testLeave.setStartTime(LocalTime.of(10, 0));
        testLeave.setDuration(DurationType.FULL_DAY);
        testLeave.setDescription("Annual leave");

        mockKimaiUser = new KimaiUserResponse();
        mockKimaiUser.setId(4);
        mockKimaiUser.setUsername("Raj");

        response = new KimaiTimesheetResponse();
        response.setId(1);
    }

    @Test
    void shouldSyncLeaveSuccessfullyForFullDayLeave() {
        when(responseSpec.bodyToMono(KimaiTimesheetResponse.class))
                .thenReturn(Mono.just(response));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class)))
                .thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class))
                .thenReturn(Flux.just(mockKimaiUser));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/timesheets")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(postHeadersSpec);
        when(postHeadersSpec.retrieve()).thenReturn(responseSpec);

        kimaiService.syncLeave(testLeave);

        verify(webClient).post();
        verify(requestBodyUriSpec).uri("/api/timesheets");
        verify(requestBodyUriSpec).bodyValue(any());
        verify(postHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(KimaiTimesheetResponse.class);
    }

    @Test
    void shouldSyncLeaveSuccessfullyForHalfDayLeave() {
        testLeave.setDuration(DurationType.HALF_DAY);
        when(responseSpec.bodyToMono(KimaiTimesheetResponse.class))
                .thenReturn(Mono.just(response));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class)))
                .thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class))
                .thenReturn(Flux.just(mockKimaiUser));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/timesheets")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(postHeadersSpec);
        when(postHeadersSpec.retrieve()).thenReturn(responseSpec);

        kimaiService.syncLeave(testLeave);

        verify(webClient).post();
        verify(requestBodyUriSpec).uri("/api/timesheets");
        verify(requestBodyUriSpec).bodyValue(any());
        verify(postHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(KimaiTimesheetResponse.class);
    }

    @Test
    void shouldHandleWebClientErrorGracefully() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class)))
                .thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class))
                .thenReturn(Flux.just(mockKimaiUser));

        when(responseSpec.bodyToMono(KimaiTimesheetResponse.class))
                .thenThrow(WebClientResponseException.create(
                        500, "Internal Server Error", null, null, null));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/timesheets")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(postHeadersSpec);
        when(postHeadersSpec.retrieve()).thenReturn(responseSpec);

        kimaiService.syncLeave(testLeave);

        verify(webClient).post();
    }

    @Test
    void shouldReturnUserIdWhenUserExists() {
        KimaiUserResponse user = new KimaiUserResponse();
        user.setId(10);
        user.setUsername("Raj");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class)))
                .thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class))
                .thenReturn(Flux.just(user));

        Integer result = kimaiService.getUserIdByEmail("raj@technogise.com", "Raj");

        assertEquals(10, result);
    }

    @Test
    void shouldHandleExceptionInUserFetch() {

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class)))
                .thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.bodyToFlux(KimaiUserResponse.class))
                .thenThrow(new RuntimeException("API Error"));

        kimaiService.syncLeave(testLeave);

        verify(webClient).get();
    }

    @Test
    void shouldReturnTrueWhenTimesheetExists() {
        LocalDateTime begin = LocalDateTime.of(2026, 5, 18, 10, 0);
        LocalDateTime end = begin.plusDays(1);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(Object.class))
                .thenReturn(Flux.just(new Object()));

        boolean result = kimaiService.isLeaveAlreadySynced(2, begin, end);

        assertTrue(result);
    }

    @Test
    void shouldSkipSyncWhenLeaveAlreadyExists() {
        KimaiUserResponse user = new KimaiUserResponse();
        user.setId(2);
        user.setUsername("Raj");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class))
                .thenReturn(Flux.just(user));

        doReturn(true).when(kimaiService)
                .isLeaveAlreadySynced(any(), any(), any());

        kimaiService.syncLeave(testLeave);

        verify(webClient, never()).post();
    }

    @Test
    void shouldSetExternalEventIdWhenResponseIsNotNull() {
        KimaiTimesheetResponse timesheetResponse = new KimaiTimesheetResponse();
        timesheetResponse.setId(42);

        KimaiUserResponse user = new KimaiUserResponse();
        user.setId(4);
        user.setUsername("Raj");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class)).thenReturn(Flux.just(user));

        doReturn(false).when(kimaiService).isLeaveAlreadySynced(any(), any(), any());

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/timesheets")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(postHeadersSpec);
        when(postHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(KimaiTimesheetResponse.class))
                .thenReturn(Mono.just(timesheetResponse));

        when(eventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        kimaiService.syncLeave(testLeave);

        verify(eventRepository).save(argThat(event ->
                "42".equals(event.getExternalEventId())
                        && event.getStatus() == IntegrationStatus.SUCCESS
        ));
    }

    @Test
    void shouldNotSetExternalEventIdWhenResponseIsNull() {
        KimaiUserResponse user = new KimaiUserResponse();
        user.setId(4);
        user.setUsername("Raj");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class)).thenReturn(Flux.just(user));

        doReturn(false).when(kimaiService).isLeaveAlreadySynced(any(), any(), any());

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/timesheets")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(postHeadersSpec);
        when(postHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(KimaiTimesheetResponse.class))
                .thenReturn(Mono.empty()); // returns null

        when(eventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        kimaiService.syncLeave(testLeave);

        verify(eventRepository).save(argThat(event ->
                event.getExternalEventId() == null
                        && event.getStatus() == IntegrationStatus.SUCCESS
        ));
    }

    @Test
    void shouldNotSetExternalEventIdWhenResponseIdIsNull() {
        KimaiTimesheetResponse timesheetResponse = new KimaiTimesheetResponse();
        timesheetResponse.setId(null); // id is null

        KimaiUserResponse user = new KimaiUserResponse();
        user.setId(4);
        user.setUsername("Raj");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class)).thenReturn(Flux.just(user));

        doReturn(false).when(kimaiService).isLeaveAlreadySynced(any(), any(), any());

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/timesheets")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(postHeadersSpec);
        when(postHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(KimaiTimesheetResponse.class))
                .thenReturn(Mono.just(timesheetResponse));

        when(eventRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        kimaiService.syncLeave(testLeave);

        verify(eventRepository).save(argThat(event ->
                event.getExternalEventId() == null
                        && event.getStatus() == IntegrationStatus.SUCCESS
        ));
    }

    private LeaveIntegrationEvent createKimaiEvent(Leave leave) {
        LeaveIntegrationEvent event = new LeaveIntegrationEvent();
        event.setLeave(leave);
        event.setPlatform(PlatformType.KIMAI);
        event.setExternalEventId("123");
        event.setStatus(IntegrationStatus.SUCCESS);
        event.setDeletedAt(null);
        return event;
    }

    private void mockKimaiDeleteSuccess(String externalId) {
        when(webClient.delete()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/timesheets/{id}", externalId))
                .thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());
    }

    @Test
    void shouldDeleteKimaiEntrySuccessfullyWhenEventExists() {
        LeaveIntegrationEvent event = createKimaiEvent(testLeave);

        when(eventRepository.findByLeaveIdAndPlatformAndDeletedAtIsNull(
                testLeave.getId(), PlatformType.KIMAI))
                .thenReturn(Optional.of(event));

        mockKimaiDeleteSuccess("123");

        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        kimaiService.deleteLeave(testLeave);

        verify(webClient).delete();
        verify(requestHeadersUriSpec).uri("/api/timesheets/{id}", "123");
        verify(getHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(Void.class);
    }

    @Test
    void shouldSaveSuccessEventAfterSuccessfulKimaiDeletion() {
        LeaveIntegrationEvent event = createKimaiEvent(testLeave);

        when(eventRepository.findByLeaveIdAndPlatformAndDeletedAtIsNull(testLeave.getId(), PlatformType.KIMAI)).thenReturn(Optional.of(event));

        mockKimaiDeleteSuccess("123");

        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        kimaiService.deleteLeave(testLeave);

        verify(eventRepository).save(argThat(savedEvent -> savedEvent.getStatus() == IntegrationStatus.SUCCESS
                && savedEvent.getExternalEventId().equals("123")));
    }

    @Test
    void shouldSaveFailedEventWhenNoIntegrationEventFound() {
        when(eventRepository.findByLeaveIdAndPlatformAndDeletedAtIsNull(testLeave.getId(), PlatformType.KIMAI))
                .thenReturn(Optional.empty());

        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        kimaiService.deleteLeave(testLeave);

        verify(webClient, never()).delete();
        verify(eventRepository).save(argThat(savedEvent -> savedEvent.getStatus() == IntegrationStatus.FAILED
                && savedEvent.getErrorMessage() != null));
    }

    @Test
    void shouldQueryRepositoryWithCorrectLeaveIdAndPlatformOnDelete() {
        when(eventRepository.findByLeaveIdAndPlatformAndDeletedAtIsNull(testLeave.getId(), PlatformType.KIMAI)).thenReturn(Optional.empty());

        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        kimaiService.deleteLeave(testLeave);

        verify(eventRepository).findByLeaveIdAndPlatformAndDeletedAtIsNull(testLeave.getId(), PlatformType.KIMAI);
    }

    @Test
    void shouldSaveFailedEventWhenKimaiDeleteCallFails() {
        LeaveIntegrationEvent event = createKimaiEvent(testLeave);

        when(eventRepository.findByLeaveIdAndPlatformAndDeletedAtIsNull(
                testLeave.getId(), PlatformType.KIMAI))
                .thenReturn(Optional.of(event));

        when(webClient.delete()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/timesheets/{id}", "123")).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenThrow(new RuntimeException("Connection refused"));

        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        kimaiService.deleteLeave(testLeave);

        verify(eventRepository).save(argThat(savedEvent -> savedEvent.getStatus() == IntegrationStatus.FAILED
                && savedEvent.getErrorMessage().equals("Connection refused")));
    }

    @Test
    void shouldSaveFailedEventWhenUserNotFoundDuringUpdate() {
        LeaveIntegrationEvent existingEvent = createKimaiEvent(testLeave);

        when(eventRepository.findFirstByLeaveIdAndPlatformAndDeletedAtIsNullOrderByCreatedAtDesc(
                testLeave.getId(), PlatformType.KIMAI))
                .thenReturn(Optional.of(existingEvent));

        when(userRepository.findById(testLeave.getUser().getId()))
                .thenReturn(Optional.empty());

        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        kimaiService.updateLeave(testLeave);

        verify(eventRepository).save(argThat(event ->
                event.getStatus() == IntegrationStatus.FAILED
                        && event.getErrorMessage().contains("User not found for leaveId=")
                        && event.getOperationType() == IntegrationOperationType.UPDATE
        ));
        verify(webClient, never()).get();
    }

    @Test
    void shouldPatchKimaiTimesheetSuccessfullyForHalfDayLeaveOnUpdate() {
        testLeave.setDuration(DurationType.HALF_DAY);
        LeaveIntegrationEvent existingEvent = createKimaiEvent(testLeave);

        when(eventRepository.findFirstByLeaveIdAndPlatformAndDeletedAtIsNullOrderByCreatedAtDesc(
                testLeave.getId(), PlatformType.KIMAI))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findById(testLeave.getUser().getId()))
                .thenReturn(Optional.of(testLeave.getUser()));

        KimaiUserResponse user = new KimaiUserResponse();
        user.setId(4);
        user.setUsername("Raj");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class)).thenReturn(Flux.just(user));

        WebClient.RequestBodyUriSpec patchUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersSpec patchHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec patchResponseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.patch()).thenReturn(patchUriSpec);
        when(patchUriSpec.uri("/api/timesheets/{id}", "123")).thenReturn(patchUriSpec);
        when(patchUriSpec.bodyValue(any())).thenReturn(patchHeadersSpec);
        when(patchHeadersSpec.retrieve()).thenReturn(patchResponseSpec);
        when(patchResponseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        kimaiService.updateLeave(testLeave);

        verify(webClient).patch();
        verify(eventRepository).save(argThat(savedEvent ->
                savedEvent.getStatus() == IntegrationStatus.SUCCESS
                        && savedEvent.getOperationType() == IntegrationOperationType.UPDATE
        ));
    }

    @Test
    void shouldFallbackToSyncLeaveWhenNoPreviousKimaiEventFound() {
        when(eventRepository.findFirstByLeaveIdAndPlatformAndDeletedAtIsNullOrderByCreatedAtDesc(
                any(), any()))
                .thenReturn(Optional.empty());

        doNothing().when(kimaiService).syncLeave(testLeave);

        kimaiService.updateLeave(testLeave);

        verify(kimaiService).syncLeave(testLeave);
        verify(webClient, never()).patch();
    }

    @Test
    void shouldFallbackToSyncLeaveWhenPreviousKimaiEventHasNoExternalId() {
        LeaveIntegrationEvent event = new LeaveIntegrationEvent();
        event.setLeave(testLeave);
        event.setPlatform(PlatformType.KIMAI);
        event.setExternalEventId(null);

        when(eventRepository.findFirstByLeaveIdAndPlatformAndDeletedAtIsNullOrderByCreatedAtDesc(
                testLeave.getId(), PlatformType.KIMAI))
                .thenReturn(Optional.of(event));

        doNothing().when(kimaiService).syncLeave(testLeave);

        kimaiService.updateLeave(testLeave);

        verify(kimaiService).syncLeave(testLeave);
        verify(webClient, never()).patch();
    }

    @Test
    void shouldHandleHolidayDisplayNameWhenLeaveCategoryIsNull() {
        Holiday optionalHoliday = new Holiday();
        optionalHoliday.setType(HolidayType.OPTIONAL);

        testLeave.setLeaveCategory(null);
        testLeave.setHoliday(optionalHoliday);

        LeaveIntegrationEvent existingEvent = createKimaiEvent(testLeave);
        when(eventRepository.findFirstByLeaveIdAndPlatformAndDeletedAtIsNullOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(existingEvent));

        when(userRepository.findById(testLeave.getUser().getId()))
                .thenReturn(Optional.of(testLeave.getUser()));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class)).thenReturn(Flux.just(mockKimaiUser));

        WebClient.RequestBodyUriSpec patchUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersSpec patchHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        when(webClient.patch()).thenReturn(patchUriSpec);
        when(patchUriSpec.uri(anyString(), anyString())).thenReturn(patchUriSpec);
        when(patchUriSpec.bodyValue(any())).thenReturn(patchHeadersSpec);
        when(patchHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        assertDoesNotThrow(() -> kimaiService.updateLeave(testLeave));

        verify(eventRepository).save(argThat(event ->
                event.getOperationType() == IntegrationOperationType.UPDATE
                        && event.getStatus() == IntegrationStatus.SUCCESS
        ));
    }

    @Test
    void shouldPatchKimaiTimesheetSuccessfullyWhenPreviousEventExists() {
        LeaveIntegrationEvent existingEvent = createKimaiEvent(testLeave);

        when(eventRepository.findFirstByLeaveIdAndPlatformAndDeletedAtIsNullOrderByCreatedAtDesc(
                testLeave.getId(), PlatformType.KIMAI))
                .thenReturn(Optional.of(existingEvent));

        when(userRepository.findById(testLeave.getUser().getId()))
                .thenReturn(Optional.of(testLeave.getUser()));

        KimaiUserResponse user = new KimaiUserResponse();
        user.setId(4);
        user.setUsername("Raj");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class)).thenReturn(Flux.just(user));

        WebClient.RequestBodyUriSpec patchUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestHeadersSpec patchHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec patchResponseSpec = mock(WebClient.ResponseSpec.class);

        when(webClient.patch()).thenReturn(patchUriSpec);
        when(patchUriSpec.uri("/api/timesheets/{id}", "123")).thenReturn(patchUriSpec);
        when(patchUriSpec.bodyValue(any())).thenReturn(patchHeadersSpec);
        when(patchHeadersSpec.retrieve()).thenReturn(patchResponseSpec);
        when(patchResponseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        kimaiService.updateLeave(testLeave);

        verify(webClient).patch();
        verify(eventRepository).save(argThat(savedEvent ->
                savedEvent.getStatus() == IntegrationStatus.SUCCESS
                        && "123".equals(savedEvent.getExternalEventId())
                        && savedEvent.getOperationType() == IntegrationOperationType.UPDATE
        ));
    }

    @Test
    void shouldSaveFailedEventWhenKimaiPatchCallFails() {
        LeaveIntegrationEvent existingEvent = createKimaiEvent(testLeave);

        when(eventRepository.findFirstByLeaveIdAndPlatformAndDeletedAtIsNullOrderByCreatedAtDesc(
                testLeave.getId(), PlatformType.KIMAI))
                .thenReturn(Optional.of(existingEvent));
        when(userRepository.findById(testLeave.getUser().getId()))
                .thenReturn(Optional.of(testLeave.getUser()));

        KimaiUserResponse user = new KimaiUserResponse();
        user.setId(4);
        user.setUsername("Raj");

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class)).thenReturn(Flux.just(user));

        WebClient.RequestBodyUriSpec patchUriSpec = mock(WebClient.RequestBodyUriSpec.class);

        when(webClient.patch()).thenReturn(patchUriSpec);
        when(patchUriSpec.uri("/api/timesheets/{id}", "123")).thenReturn(patchUriSpec);
        when(patchUriSpec.bodyValue(any())).thenThrow(new RuntimeException("Kimai patch failed"));

        when(eventRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        kimaiService.updateLeave(testLeave);

        verify(eventRepository).save(argThat(savedEvent ->
                savedEvent.getStatus() == IntegrationStatus.FAILED
                        && savedEvent.getErrorMessage().equals("Kimai patch failed")
                        && savedEvent.getOperationType() == IntegrationOperationType.UPDATE
        ));
    }

    @Test
    void shouldHandleMissingActivityMappingDuringUpdate() {
        LeaveCategory unknownCategory = new LeaveCategory();
        unknownCategory.setName("Unknown Category");
        testLeave.setLeaveCategory(unknownCategory);

        LeaveIntegrationEvent existingEvent = createKimaiEvent(testLeave);
        when(eventRepository.findFirstByLeaveIdAndPlatformAndDeletedAtIsNullOrderByCreatedAtDesc(any(), any()))
                .thenReturn(Optional.of(existingEvent));

        when(userRepository.findById(testLeave.getUser().getId()))
                .thenReturn(Optional.of(testLeave.getUser()));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class)).thenReturn(Flux.just(mockKimaiUser));

        kimaiService.updateLeave(testLeave);

        verify(eventRepository).save(argThat(event ->
                event.getStatus() == IntegrationStatus.FAILED
                       && event.getErrorMessage().equals("No Kimai activity mapping found for: Unknown Category")
                      &&  event.getOperationType() == IntegrationOperationType.UPDATE
        ));

        verify(webClient, never()).patch();
    }
}
