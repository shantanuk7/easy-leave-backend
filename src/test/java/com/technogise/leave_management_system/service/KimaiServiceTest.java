package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveCategory;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KimaiServiceTest {
    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    private KimaiService kimaiService;

    private Leave testLeave;

    @BeforeEach
    void setUp() {
        kimaiService = new KimaiService(webClient);

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
    }

    @Test
    void shouldSyncLeaveSuccessfullyForFullDayLeave() {
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/timesheets")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        kimaiService.syncLeave(testLeave);

        verify(webClient).post();
        verify(requestBodyUriSpec).uri("/api/timesheets");
        verify(requestBodyUriSpec).bodyValue(any());
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(Void.class);
    }

    @Test
    void shouldSyncLeaveSuccessfullyForHalfDayLeave() {
        testLeave.setDuration(DurationType.HALF_DAY);
        when(responseSpec.bodyToMono(Void.class)).thenReturn(Mono.empty());

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/timesheets")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        kimaiService.syncLeave(testLeave);

        verify(webClient).post();
        verify(requestBodyUriSpec).uri("/api/timesheets");
        verify(requestBodyUriSpec).bodyValue(any());
        verify(requestHeadersSpec).retrieve();
        verify(responseSpec).bodyToMono(Void.class);
    }

    @Test
    void shouldHandleWebClientErrorGracefully() {
        when(responseSpec.bodyToMono(Void.class))
                .thenThrow(WebClientResponseException.create(
                        500, "Internal Server Error", null, null, null));

        when(webClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri("/api/timesheets")).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);

        kimaiService.syncLeave(testLeave);

        verify(webClient).post();
    }
}
