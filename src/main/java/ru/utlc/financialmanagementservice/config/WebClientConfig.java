package ru.utlc.financialmanagementservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient clientManagementServiceWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("http://localhost:8000")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }
}
