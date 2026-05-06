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
import com.technogise.leave_management_system.enums.IntegrationOperationType;
import com.technogise.leave_management_system.enums.HolidayType;

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
    private WebClient.RequestHeadersSpec requestHeadersSpec;

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
    private KimaiTimesheetResponse kimaiResponse;

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

        kimaiResponse = new KimaiTimesheetResponse();
        kimaiResponse.setId(1);
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

    @Test
    void shouldSyncLeaveSuccessfullyForFullDayLeave() {
        when(responseSpec.bodyToMono(KimaiTimesheetResponse.class))
                .thenReturn(Mono.just(kimaiResponse));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class)))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class))
                .thenReturn(Flux.just(mockKimaiUser));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/timesheets")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        kimaiService.syncLeave(testLeave);

        verify(webClient).post();
        verify(requestBodyUriSpec).uri("/api/timesheets");
        verify(requestBodyUriSpec).bodyValue(any());
        verify(requestHeadersSpec, atLeastOnce()).retrieve();
        verify(responseSpec).bodyToMono(KimaiTimesheetResponse.class);
    }

    @Test
    void shouldSyncLeaveSuccessfullyForHalfDayLeave() {
        testLeave.setDuration(DurationType.HALF_DAY);
        when(responseSpec.bodyToMono(KimaiTimesheetResponse.class))
                .thenReturn(Mono.just(kimaiResponse));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class)))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class))
                .thenReturn(Flux.just(mockKimaiUser));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/timesheets")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        kimaiService.syncLeave(testLeave);

        verify(webClient).post();
        verify(requestBodyUriSpec).uri("/api/timesheets");
        verify(requestBodyUriSpec).bodyValue(any());
        verify(requestHeadersSpec, atLeastOnce()).retrieve();
        verify(responseSpec).bodyToMono(KimaiTimesheetResponse.class);
    }

    @Test
    void shouldHandleWebClientErrorGracefully() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class)))
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class))
                .thenReturn(Flux.just(mockKimaiUser));

        when(responseSpec.bodyToMono(KimaiTimesheetResponse.class))
                .thenThrow(WebClientResponseException.create(
                        500, "Internal Server Error", null, null, null));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/timesheets")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

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
                .thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class))
                .thenReturn(Flux.just(user));

        Integer result = kimaiService.getUserIdByEmail("raj@technogise.com", "Raj");

        assertEquals(10, result);
    }

    @Test
    void shouldLogAndRethrowExceptionWhenKimaiUserFetchFails() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class))
                .thenThrow(new RuntimeException("Connection Error"));

        assertThrows(RuntimeException.class, () ->
                kimaiService.getUserIdByEmail("raj@technogise.com", "Raj"));
    }

    @Test
    void shouldReturnTrueWhenTimesheetExists() {
        LocalDateTime begin = LocalDateTime.of(2026, 5, 18, 10, 0);
        LocalDateTime end = begin.plusDays(1);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
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
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class))
                .thenReturn(Flux.just(user));

        doReturn(true).when(kimaiService)
                .isLeaveAlreadySynced(any(), any(), any());

        kimaiService.syncLeave(testLeave);

        verify(webClient, never()).post();
    }

    @Test
    void shouldNotSetExternalEventIdWhenKimaiResponseIdIsNull() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class)).thenReturn(Flux.just(mockKimaiUser));

        doReturn(false).when(kimaiService).isLeaveAlreadySynced(any(), any(), any());

        KimaiTimesheetResponse emptyIdResponse = new KimaiTimesheetResponse();
        emptyIdResponse.setId(null);

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/timesheets")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(KimaiTimesheetResponse.class))
                .thenReturn(Mono.just(emptyIdResponse));

        kimaiService.syncLeave(testLeave);

        verify(eventRepository).save(argThat(event ->
                event.getStatus() == IntegrationStatus.SUCCESS
                        && event.getExternalEventId() == null
        ));
    }

    @Test
    void shouldNotSetExternalEventIdWhenKimaiResponseIsCompletelyEmpty() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class)).thenReturn(Flux.just(mockKimaiUser));

        doReturn(false).when(kimaiService).isLeaveAlreadySynced(any(), any(), any());

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/timesheets")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(KimaiTimesheetResponse.class))
                .thenReturn(Mono.empty());

        kimaiService.syncLeave(testLeave);

        verify(eventRepository).save(argThat(event ->
                event.getStatus() == IntegrationStatus.SUCCESS
                        && event.getExternalEventId() == null
        ));
    }

    @Test
    void shouldDeleteKimaiEntrySuccessfullyWhenEventExists() {
        LeaveIntegrationEvent event = createKimaiEvent(testLeave);

        when(eventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                testLeave.getId(), PlatformType.KIMAI, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.of(event));

        when(userRepository.findById(testLeave.getUser().getId())).thenReturn(Optional.of(testLeave.getUser()));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class)).thenReturn(Flux.just(mockKimaiUser));

        when(webClient.delete()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/timesheets/{id}", "123")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        kimaiService.deleteLeave(testLeave);

        verify(webClient).delete();
        verify(eventRepository).save(argThat(savedEvent ->
                savedEvent.getStatus() == IntegrationStatus.SUCCESS
                        && savedEvent.getOperationType() == IntegrationOperationType.DELETE
        ));
    }

    @Test
    void shouldSaveFailedEventWhenUserIsNotFoundInRepositoryDuringDelete() {
        LeaveIntegrationEvent existingEvent = createKimaiEvent(testLeave);

        when(eventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                testLeave.getId(), PlatformType.KIMAI, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.of(existingEvent));

        when(userRepository.findById(testLeave.getUser().getId())).thenReturn(Optional.empty());

        kimaiService.deleteLeave(testLeave);

        verify(eventRepository).save(argThat(event ->
                event.getStatus() == IntegrationStatus.FAILED
                        && event.getErrorMessage().equals("User not found for leaveId=" + testLeave.getId())
                        && event.getOperationType() == IntegrationOperationType.DELETE
        ));
    }

    @Test
    void shouldSaveSuccessEventAfterSuccessfulKimaiDeletion() {
        LeaveIntegrationEvent event = createKimaiEvent(testLeave);

        when(eventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                testLeave.getId(), PlatformType.KIMAI, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.of(event));

        when(userRepository.findById(testLeave.getUser().getId())).thenReturn(Optional.of(testLeave.getUser()));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class)).thenReturn(Flux.just(mockKimaiUser));

        when(webClient.delete()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/timesheets/{id}", "123")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        kimaiService.deleteLeave(testLeave);

        verify(eventRepository).save(argThat(savedEvent ->
                savedEvent.getStatus() == IntegrationStatus.SUCCESS
                        && "123".equals(savedEvent.getExternalEventId())
                        && savedEvent.getOperationType() == IntegrationOperationType.DELETE));
    }

    @Test
    void shouldSaveFailedEventWhenNoIntegrationEventFound() {
        when(eventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                testLeave.getId(), PlatformType.KIMAI, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.empty());

        kimaiService.deleteLeave(testLeave);

        verify(webClient, never()).delete();
        verify(eventRepository).save(argThat(savedEvent ->
                savedEvent.getStatus() == IntegrationStatus.FAILED
                        && savedEvent.getErrorMessage().contains("No Kimai entry found")
        ));
    }

    @Test
    void shouldQueryRepositoryWithCorrectLeaveIdAndPlatformOnDelete() {
        when(eventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                testLeave.getId(), PlatformType.KIMAI, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.empty());

        kimaiService.deleteLeave(testLeave);

        verify(eventRepository).findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                testLeave.getId(), PlatformType.KIMAI, IntegrationStatus.SUCCESS);
    }

    @Test
    void shouldSaveFailedEventWhenKimaiDeleteCallFails() {
        LeaveIntegrationEvent event = createKimaiEvent(testLeave);

        when(eventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                testLeave.getId(), PlatformType.KIMAI, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.of(event));

        when(userRepository.findById(testLeave.getUser().getId())).thenReturn(Optional.of(testLeave.getUser()));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class)).thenReturn(Flux.just(mockKimaiUser));

        when(webClient.delete()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/api/timesheets/{id}", "123")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenThrow(new RuntimeException("Connection refused"));

        kimaiService.deleteLeave(testLeave);

        verify(eventRepository).save(argThat(savedEvent ->
                savedEvent.getStatus() == IntegrationStatus.FAILED
                        && "Connection refused".equals(savedEvent.getErrorMessage())
                        && savedEvent.getOperationType() == IntegrationOperationType.DELETE));
    }

    @Test
    void shouldSaveFailedEventWhenUserNotFoundDuringUpdate() {
        LeaveIntegrationEvent existingEvent = createKimaiEvent(testLeave);

        when(eventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                testLeave.getId(), PlatformType.KIMAI, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.of(existingEvent));

        when(userRepository.findById(testLeave.getUser().getId())).thenReturn(Optional.empty());

        kimaiService.updateLeave(testLeave);

        verify(eventRepository).save(argThat(event ->
                event.getStatus() == IntegrationStatus.FAILED
                        && event.getErrorMessage().contains("User not found for leaveId=")
                        && event.getOperationType() == IntegrationOperationType.UPDATE
        ));
    }

    @Test
    void shouldPatchKimaiTimesheetSuccessfullyForHalfDayLeaveOnUpdate() {
        testLeave.setDuration(DurationType.HALF_DAY);
        LeaveIntegrationEvent existingEvent = createKimaiEvent(testLeave);

        when(eventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                testLeave.getId(), PlatformType.KIMAI, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.of(existingEvent));

        when(userRepository.findById(testLeave.getUser().getId())).thenReturn(Optional.of(testLeave.getUser()));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class)).thenReturn(Flux.just(mockKimaiUser));

        WebClient.RequestBodyUriSpec patchUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        when(webClient.patch()).thenReturn(patchUriSpec);
        when(patchUriSpec.uri(anyString(), anyString())).thenReturn(patchUriSpec);
        when(patchUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        kimaiService.updateLeave(testLeave);

        verify(webClient).patch();
        verify(eventRepository).save(argThat(savedEvent ->
                savedEvent.getStatus() == IntegrationStatus.SUCCESS
                        && savedEvent.getOperationType() == IntegrationOperationType.UPDATE
        ));
    }

    @Test
    void shouldFallbackToSyncLeaveWhenNoPreviousKimaiEventFound() {
        when(eventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                any(), any(), eq(IntegrationStatus.SUCCESS)))
                .thenReturn(Optional.empty());

        doNothing().when(kimaiService).syncLeave(testLeave);

        kimaiService.updateLeave(testLeave);

        verify(kimaiService).syncLeave(testLeave);
    }

    @Test
    void shouldFallbackToSyncLeaveWhenPreviousKimaiEventHasNoExternalId() {
        LeaveIntegrationEvent event = new LeaveIntegrationEvent();
        event.setLeave(testLeave);
        event.setPlatform(PlatformType.KIMAI);
        event.setExternalEventId(null);
        event.setStatus(IntegrationStatus.SUCCESS);

        when(eventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                testLeave.getId(), PlatformType.KIMAI, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.of(event));

        doNothing().when(kimaiService).syncLeave(testLeave);

        kimaiService.updateLeave(testLeave);

        verify(kimaiService).syncLeave(testLeave);
    }

    @Test
    void shouldHandleHolidayDisplayNameWhenLeaveCategoryIsNull() {
        Holiday optionalHoliday = new Holiday();
        optionalHoliday.setType(HolidayType.OPTIONAL);

        testLeave.setLeaveCategory(null);
        testLeave.setHoliday(optionalHoliday);

        LeaveIntegrationEvent existingEvent = createKimaiEvent(testLeave);
        when(eventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                any(), any(), eq(IntegrationStatus.SUCCESS)))
                .thenReturn(Optional.of(existingEvent));

        when(userRepository.findById(testLeave.getUser().getId())).thenReturn(Optional.of(testLeave.getUser()));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class)).thenReturn(Flux.just(mockKimaiUser));

        WebClient.RequestBodyUriSpec patchUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        when(webClient.patch()).thenReturn(patchUriSpec);
        when(patchUriSpec.uri(anyString(), anyString())).thenReturn(patchUriSpec);
        when(patchUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        kimaiService.updateLeave(testLeave);

        verify(eventRepository).save(argThat(event ->
                event.getOperationType() == IntegrationOperationType.UPDATE
                        && event.getStatus() == IntegrationStatus.SUCCESS
        ));
    }

    @Test
    void shouldPatchKimaiTimesheetSuccessfullyWhenPreviousEventExists() {
        LeaveIntegrationEvent existingEvent = createKimaiEvent(testLeave);

        when(eventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                testLeave.getId(), PlatformType.KIMAI, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.of(existingEvent));

        when(userRepository.findById(testLeave.getUser().getId())).thenReturn(Optional.of(testLeave.getUser()));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class)).thenReturn(Flux.just(mockKimaiUser));

        WebClient.RequestBodyUriSpec patchUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        when(webClient.patch()).thenReturn(patchUriSpec);
        when(patchUriSpec.uri(anyString(), anyString())).thenReturn(patchUriSpec);
        when(patchUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

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

        when(eventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                testLeave.getId(), PlatformType.KIMAI, IntegrationStatus.SUCCESS))
                .thenReturn(Optional.of(existingEvent));

        when(userRepository.findById(testLeave.getUser().getId())).thenReturn(Optional.of(testLeave.getUser()));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class)).thenReturn(Flux.just(mockKimaiUser));

        WebClient.RequestBodyUriSpec patchUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        when(webClient.patch()).thenReturn(patchUriSpec);
        when(patchUriSpec.uri(anyString(), anyString())).thenReturn(patchUriSpec);
        when(patchUriSpec.bodyValue(any())).thenThrow(new RuntimeException("Kimai patch failed"));

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
        when(eventRepository.findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                any(), any(), eq(IntegrationStatus.SUCCESS)))
                .thenReturn(Optional.of(existingEvent));

        when(userRepository.findById(testLeave.getUser().getId())).thenReturn(Optional.of(testLeave.getUser()));

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class)).thenReturn(Flux.just(mockKimaiUser));

        kimaiService.updateLeave(testLeave);

        verify(eventRepository).save(argThat(event ->
                event.getStatus() == IntegrationStatus.FAILED
                        && event.getErrorMessage().equals("No Kimai activity mapping found for: Unknown Category")
                        && event.getOperationType() == IntegrationOperationType.UPDATE
        ));

        verify(webClient, never()).patch();
    }
}
