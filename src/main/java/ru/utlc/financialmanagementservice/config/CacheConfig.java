package ru.utlc.financialmanagementservice.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static ru.utlc.financialmanagementservice.constants.CacheNames.*;

@Configuration
@EnableCaching
public class CacheConfig {
    public CacheConfig() {
    }

    @Bean
    public ConcurrentMapCacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                INVOICES,
                INVOICE_STATUSES,
                CURRENCIES,
                PAYMENTS,
                PAYMENT_TYPES,
                SERVICE_TYPES);
    }
}
