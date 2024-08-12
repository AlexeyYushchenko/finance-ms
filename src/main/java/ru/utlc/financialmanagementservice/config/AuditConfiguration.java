package ru.utlc.financialmanagementservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.ReactiveAuditorAware;
import org.springframework.data.r2dbc.config.EnableR2dbcAuditing;

@Configuration
@EnableR2dbcAuditing
public class AuditConfiguration {


    @Bean
    public ReactiveAuditorAware<String> auditorAware() {
        return SecurityContextUtil::getCurrentAuditor;
    }
}
