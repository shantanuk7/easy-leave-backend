package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.dto.KimaiCreateLeaveRequest;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.enums.DurationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;

@Service
@Slf4j
public class KimaiService implements LeaveIntegrationService {
    private final WebClient webClient;

    public KimaiService(WebClient kimaiWebClient) {
        this.webClient = kimaiWebClient;
    }

    @Override
    @Async
    public void syncLeave(Leave leave) {
        try {
            LocalDateTime begin = LocalDateTime.of(
                    leave.getDate(),
                    leave.getStartTime()
            );
            LocalDateTime end = leave.getDuration() == DurationType.HALF_DAY
                    ? begin.plusHours(4)
                    : begin.plusHours(8);

            KimaiCreateLeaveRequest request = KimaiCreateLeaveRequest.builder()
                    .begin(begin.toString())
                    .end(end.toString())
                    .project(2)
                    .activity(4)
                    .description(leave.getDescription())
                    .user(4)
                    .build();

            webClient.post()
                    .uri("/api/timesheets")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            log.info("Successfully synced leave with Kimai for user {}", leave.getUser().getName());
        } catch (Exception e) {
            log.error("Error syncing leave with Kimai: {}", e.getMessage());
        }
    }
}
