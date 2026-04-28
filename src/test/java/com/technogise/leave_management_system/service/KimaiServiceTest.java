package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.KimaiUserResponse;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.UserRole;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KimaiServiceTest {
    @Mock
    private WebClient webClient;

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

    private Leave testLeave;
    private KimaiUserResponse mockKimaiUser;

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
    }

    @Test
    void shouldSyncLeaveSuccessfullyForFullDayLeave() {
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

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
        verify(responseSpec).bodyToMono(Void.class);
    }

    @Test
    void shouldSyncLeaveSuccessfullyForHalfDayLeave() {
        testLeave.setDuration(DurationType.HALF_DAY);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

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
        verify(responseSpec).bodyToMono(Void.class);
    }

    @Test
    void shouldHandleWebClientErrorGracefully() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(String.class)))
                .thenReturn(getHeadersSpec);
        when(getHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToFlux(KimaiUserResponse.class))
                .thenReturn(Flux.just(mockKimaiUser));

        when(responseSpec.bodyToMono(Void.class))
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
}
