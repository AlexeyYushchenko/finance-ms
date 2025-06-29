package ru.utlc.financialmanagementservice.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static ru.utlc.financialmanagementservice.constants.CacheNames.*;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */
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
                PAYMENT_STATUSES,
                CURRENCIES,
                PAYMENTS,
                PARTNERS,
                PARTNER_TYPES,
                PAYMENT_TYPES,
                SERVICE_TYPES,
                CLIENT_BALANCES);
    }
}
