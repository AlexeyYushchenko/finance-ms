package ru.utlc.financialmanagementservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */
@Configuration
@EnableR2dbcAuditing
public class AuditConfiguration {


    @Bean
    public ReactiveAuditorAware<String> auditorAware() {
        return SecurityContextUtil::getCurrentAuditor;
    }
}
