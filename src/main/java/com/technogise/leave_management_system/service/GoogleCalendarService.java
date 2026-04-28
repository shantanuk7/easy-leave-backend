package com.technogise.leave_management_system.service;

import com.technogise.leave_management_system.entity.Leave;
import com.technogise.leave_management_system.entity.LeaveIntegrationEvent;
import com.technogise.leave_management_system.entity.User;
import com.technogise.leave_management_system.enums.IntegrationStatus;
import com.technogise.leave_management_system.enums.PlatformType;
import com.technogise.leave_management_system.exception.HttpException;
import com.technogise.leave_management_system.repository.LeaveIntegrationEventRepository;
import com.technogise.leave_management_system.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@Slf4j
public class GoogleCalendarService implements LeaveIntegrationService {

    private final UserRepository userRepository;
    private final LeaveIntegrationEventRepository leaveIntegrationEventRepository;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${google.calendar.api-base}")
    private String calendarApiBase;

    @Value("${google.calendar.timezone}")
    private String timezone;

    @Value("${google.calendar.id}")
    private String calendarId;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;

    public GoogleCalendarService(UserRepository userRepository,
                                 LeaveIntegrationEventRepository leaveIntegrationEventRepository) {
        this.userRepository = userRepository;
        this.leaveIntegrationEventRepository = leaveIntegrationEventRepository;
    }

    private String getValidToken(User user) throws Exception {
        if (user.getGoogleTokenExpiry() != null
                && user.getGoogleTokenExpiry().isAfter(LocalDateTime.now().plusMinutes(5))) {
            return user.getGoogleAccessToken();
        }

        if (user.getGoogleRefreshToken() == null) {
            throw new RuntimeException("No refresh token available for user: " + user.getEmail());
        }

        String body = "grant_type=refresh_token"
                + "&refresh_token=" + user.getGoogleRefreshToken()
                + "&client_id=" + clientId
                + "&client_secret=" + clientSecret;

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        Map<String, Object> tokenResponse = objectMapper.readValue(response.body(), Map.class);
        String newAccessToken = (String) tokenResponse.get("access_token");
        int expiresIn = (int) tokenResponse.get("expires_in");


        user.setGoogleAccessToken(newAccessToken);
        user.setGoogleTokenExpiry(LocalDateTime.now().plusSeconds(expiresIn));
        userRepository.save(user);

        return newAccessToken;
    }

    private String buildEventJson(String title, String description, String start, String end) throws Exception {
        Map<String, Object> event = Map.of(
                "summary", title,
                "description", description,
                "start", Map.of("date", start, "timeZone", timezone),
                "end",   Map.of("date", end,   "timeZone", timezone)
        );
        return objectMapper.writeValueAsString(event);
    }

    public void addLeaveEvent(User user, Leave leave, String title, String description) {
        LeaveIntegrationEvent integrationEvent = new LeaveIntegrationEvent();
        integrationEvent.setLeave(leave);
        integrationEvent.setPlatform(PlatformType.GOOGLE_CALENDAR);
        integrationEvent.setAttempts(1);
        integrationEvent.setLastAttemptAt(LocalDateTime.now());

        try {
            String encodedCalendarId = java.net.URLEncoder.encode(calendarId, "UTF-8");
            String url = calendarApiBase + encodedCalendarId + "/events";

            String start = leave.getDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String end = leave.getDate().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + getValidToken(user))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildEventJson(title, description, start, end)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201) {
                Map<String, Object> eventResponse = objectMapper.readValue(response.body(), Map.class);
                String eventId = (String) eventResponse.get("id");

                integrationEvent.setStatus(IntegrationStatus.SUCCESS);
                integrationEvent.setExternalEventId(eventId);
                leaveIntegrationEventRepository.save(integrationEvent);
            } else {
                integrationEvent.setStatus(IntegrationStatus.FAILED);
                integrationEvent.setErrorMessage("Google Calendar API error: " + response.statusCode());

                log.error("Failed to add event to Google Calendar for user");
                throw new HttpException(HttpStatus.BAD_REQUEST, "Google Calendar API error: " + response.statusCode());
            }

        } catch (Exception e) {
            integrationEvent.setStatus(IntegrationStatus.FAILED);
            integrationEvent.setErrorMessage(e.getMessage());

            log.error("Exception while adding event to Google Calendar: {}", e.getMessage());
            throw new HttpException(HttpStatus.BAD_REQUEST,"Failed to add leave to calendar" + e.getMessage());
        }
    }

    @Async
    @Override
    public void syncLeave(Leave leave) {
        User user = leave.getUser();
        String title = user.getName() + " - " + leave.getLeaveCategory().getName();
        String description = leave.getDescription();
        addLeaveEvent(user, leave, title, description);
    }
}
