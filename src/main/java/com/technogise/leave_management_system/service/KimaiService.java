package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.constants.KimaiConstants;
import com.technogise.leave_management_system.dto.KimaiCreateLeaveRequest;
import com.technogise.leave_management_system.dto.KimaiUserResponse;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.enums.DurationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
            Integer userId = getUserIdByEmail(leave.getUser().getEmail(), leave.getUser().getName());
            Integer activityId = KimaiConstants.ACTIVITY_MAPPING
                    .get(leave.getLeaveCategory().getName());

            LocalDateTime begin = LocalDateTime.of(
                    leave.getDate(),
                    leave.getStartTime()
            );
            LocalDateTime end = leave.getDuration() == DurationType.HALF_DAY
                    ? begin.plusHours(KimaiConstants.HALF_DAY_HOURS)
                    : begin.plusHours(KimaiConstants.FULL_DAY_HOURS);

            if (isLeaveAlreadySynced(userId, begin, end)) {
                log.info("Leave already synced in Kimai for user {} on {}",
                        leave.getUser().getName(), begin.toLocalDate());
                return;
            }

            KimaiCreateLeaveRequest request = KimaiCreateLeaveRequest.builder()
                    .begin(begin.toString())
                    .end(end.toString())
                    .project(KimaiConstants.LEAVE_PROJECT_ID)
                    .activity(activityId)
                    .description(leave.getDescription())
                    .user(userId)
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

    public Integer getUserIdByEmail(String email, String name) {
        try {
            String uri = "/api/users?term=" + email;
            return webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToFlux(KimaiUserResponse.class)
                    .filter(user -> user.getUsername().equalsIgnoreCase(name))
                    .map(KimaiUserResponse::getId)
                    .next()
                    .block();
        } catch (Exception e) {
            log.error("Error fetching user from Kimai: {}", e.getMessage());
            throw e;
        }
    }

    public boolean isLeaveAlreadySynced(Integer userId, LocalDateTime begin, LocalDateTime end) {
        try {
            String beginStr = begin.format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            );

            String endStr = end.format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            );

            String uri = "/api/timesheets?begin=" + beginStr + "&end=" + endStr + "&user=" + userId;

            Boolean result = webClient.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToFlux(Object.class)
                    .hasElements()
                    .block();

            return Boolean.TRUE.equals(result);

        } catch (Exception e) {
            log.error("Error checking existing timesheet: {}", e.getMessage());
            return false;
        }
    }
}
