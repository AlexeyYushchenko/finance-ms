package ru.utlc.financialmanagementservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */
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
