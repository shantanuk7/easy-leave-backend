package com.technogise.leave_management_system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient kimaiWebClient(
            @Value("${kimai.base-url}") String baseUrl,
            @Value("${kimai.api-token}") String apiToken
    ) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiToken)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
