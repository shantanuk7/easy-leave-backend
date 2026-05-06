package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.constants.KimaiConstants;
import com.technogise.leave_management_system.dto.KimaiCreateLeaveRequest;
import com.technogise.leave_management_system.dto.KimaiTimesheetResponse;
import com.technogise.leave_management_system.dto.KimaiUserResponse;
import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveIntegrationEvent;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.DurationType;
import com.technogise.leave_management_system.enums.IntegrationOperationType;
import com.technogise.leave_management_system.enums.IntegrationStatus;
import com.technogise.leave_management_system.enums.PlatformType;
import com.technogise.leave_management_system.repository.LeaveIntegrationEventRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Service
@Slf4j
public class KimaiService implements LeaveIntegrationService {
    private final WebClient webClient;
    private final LeaveIntegrationEventRepository eventRepository;
    private final UserRepository userRepository;

    public KimaiService(WebClient kimaiWebClient,
                        LeaveIntegrationEventRepository eventRepository,
                        UserRepository userRepository
    ) {
        this.webClient = kimaiWebClient;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Async
    public void syncLeave(Leave leave) {
        LeaveIntegrationEvent event = new LeaveIntegrationEvent();
        event.setLeave(leave);
        event.setPlatform(PlatformType.KIMAI);
        event.setAttempts(1);
        event.setLastAttemptAt(LocalDateTime.now());

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
                event.setStatus(IntegrationStatus.SUCCESS);
                eventRepository.save(event);
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

            KimaiTimesheetResponse response = webClient.post()
                    .uri("/api/timesheets")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(KimaiTimesheetResponse.class)
                    .block();
            log.info("Kimai raw response: {}", response);

            if (response != null && response.getId() != null) {
                event.setExternalEventId(String.valueOf(response.getId()));
            }
            event.setStatus(IntegrationStatus.SUCCESS);
            event.setErrorMessage(null);
            event.setOperationType(IntegrationOperationType.CREATE);

            log.info("Successfully synced leave with Kimai for user {}", leave.getUser().getName());
        } catch (Exception e) {
            event.setStatus(IntegrationStatus.FAILED);
            event.setErrorMessage(e.getMessage());
            event.setOperationType(IntegrationOperationType.CREATE);
            log.error("Error syncing leave with Kimai: {}", e.getMessage());
        }
        eventRepository.save(event);
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
                    DateTimeFormatter.ofPattern(KimaiConstants.KIMAI_DATE_TIME_PATTERN)
            );

            String endStr = end.format(
                    DateTimeFormatter.ofPattern(KimaiConstants.KIMAI_DATE_TIME_PATTERN)
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

    @Override
    @Async
    public void deleteLeave(Leave leave) {
        LeaveIntegrationEvent event = new LeaveIntegrationEvent();
        event.setLeave(leave);
        event.setPlatform(PlatformType.KIMAI);
        event.setAttempts(1);
        event.setLastAttemptAt(LocalDateTime.now());
        event.setOperationType(IntegrationOperationType.DELETE);

        try {
            Optional<LeaveIntegrationEvent> kimaiEvent = eventRepository
                    .findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                            leave.getId(),
                            PlatformType.KIMAI,
                            IntegrationStatus.SUCCESS);

            if (kimaiEvent.isEmpty()) {
                log.warn("No Kimai entry found for leave {}", leave.getId());
                event.setStatus(IntegrationStatus.FAILED);
                event.setErrorMessage("No Kimai entry found for leave " + leave.getId());
                eventRepository.save(event);
                return;
            }

            String kimaiId = kimaiEvent.get().getExternalEventId();

            User user = userRepository.findById(leave.getUser().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "User not found for leaveId=" + leave.getId()));

            String kimaiUserId = String.valueOf(getUserIdByEmail(user.getEmail(), user.getName()));

            webClient.delete().uri("/api/timesheets/{id}", kimaiId).retrieve().bodyToMono(Void.class).block();

            event.setExternalEventId(kimaiId);
            event.setStatus(IntegrationStatus.SUCCESS);
            event.setErrorMessage(null);

            log.info("Successfully deleted Kimai entry {} for leave {}", kimaiId, leave.getId());
        } catch (Exception e) {
            log.error("Error deleting Kimai entry for leave {}: {}", leave.getId(), e.getMessage());
            event.setStatus(IntegrationStatus.FAILED);
            event.setErrorMessage(e.getMessage());
        }
        eventRepository.save(event);
    }

    @Override
    @Async
    public void updateLeave(Leave leave) {
        Optional<LeaveIntegrationEvent> previousEvent = eventRepository
                .findFirstByLeaveIdAndPlatformAndStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                        leave.getId(),
                        PlatformType.KIMAI,
                        IntegrationStatus.SUCCESS);

        if (previousEvent.isEmpty() || previousEvent.get().getExternalEventId() == null) {
            log.warn("No Kimai ID found for update. Attempting fresh sync for leaveId={}", leave.getId());
            syncLeave(leave);
            return;
        }

        String kimaiId = previousEvent.get().getExternalEventId();

        LeaveIntegrationEvent updateEvent = new LeaveIntegrationEvent();
        updateEvent.setLeave(leave);
        updateEvent.setPlatform(PlatformType.KIMAI);
        updateEvent.setOperationType(IntegrationOperationType.UPDATE);
        updateEvent.setExternalEventId(kimaiId);
        updateEvent.setAttempts(1);
        updateEvent.setLastAttemptAt(LocalDateTime.now());

        try {
            User user = userRepository.findById(leave.getUser().getId())
                    .orElseThrow(() -> new RuntimeException(
                            "User not found for leaveId=" + leave.getId()));

            Integer kimaiUserId = getUserIdByEmail(user.getEmail(), user.getName());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(KimaiConstants.KIMAI_DATE_TIME_PATTERN);

            String categoryName = (leave.getLeaveCategory() != null)
                    ? leave.getLeaveCategory().getName()
                    : leave.getHoliday().getType().getDisplayName();

            Integer activityId = KimaiConstants.ACTIVITY_MAPPING.get(categoryName);

            if (activityId == null) {
                throw new RuntimeException("No Kimai activity mapping found for: " + categoryName);
            }

            LocalDateTime begin = LocalDateTime.of(leave.getDate(), leave.getStartTime());
            LocalDateTime end = leave.getDuration() == DurationType.HALF_DAY
                    ? begin.plusHours(KimaiConstants.HALF_DAY_HOURS)
                    : begin.plusHours(KimaiConstants.FULL_DAY_HOURS);

            KimaiCreateLeaveRequest updatePayload = KimaiCreateLeaveRequest.builder()
                    .begin(begin.format(formatter))
                    .end(end.format(formatter))
                    .project(KimaiConstants.LEAVE_PROJECT_ID)
                    .activity(activityId)
                    .description(leave.getDescription())
                    .user(kimaiUserId)
                    .build();

            webClient.patch()
                    .uri("/api/timesheets/{id}", kimaiId)
                    .bodyValue(updatePayload)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();

            updateEvent.setStatus(IntegrationStatus.SUCCESS);
            log.info("Successfully patched Kimai timesheet {} for leaveId={}", kimaiId, leave.getId());

        } catch (Exception e) {
            updateEvent.setStatus(IntegrationStatus.FAILED);
            updateEvent.setErrorMessage(e.getMessage());
            log.error("Failed to update Kimai for leaveId={}: {}", leave.getId(), e.getMessage());
        }

        eventRepository.save(updateEvent);
    }
}
