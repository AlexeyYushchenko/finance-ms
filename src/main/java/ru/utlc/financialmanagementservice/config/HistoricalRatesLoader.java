package ru.utlc.financialmanagementservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.utlc.financialmanagementservice.service.ExchangeRateService;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HistoricalRatesLoader implements CommandLineRunner {

    private final ExchangeRateService exchangeRateService;

    @Override
    public void run(String... args) {
        // Kick off the back-fill with a safe -- but plenty snappy -- concurrency of 8
        exchangeRateService.fetchAndSaveRatesForPastYear(8)
                .subscribe(
                        unused -> { },
                        error -> log.error("Failed loading historical data: {}", error.getMessage()),
                        ()   -> log.info("Successfully loaded historical currency rates!"));
    }
}
