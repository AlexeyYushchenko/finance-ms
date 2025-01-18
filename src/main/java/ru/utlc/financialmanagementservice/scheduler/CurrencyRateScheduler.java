package ru.utlc.financialmanagementservice.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.service.CurrencyRateService;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.TimeZone;

@Slf4j
@Component
@RequiredArgsConstructor
public class CurrencyRateScheduler {

    private final CurrencyRateService currencyRateService;

    @Scheduled(cron = "0 0 9-21 * * *") // Run at the start of every hour between 9 AM and 6 PM
    public void fetchDailyRates() {
        log.info("Checking if currency rates need to be updated for today...");

        currencyRateService.checkIfRatesUpdatedForToday()
                .flatMap(isUpdated -> {
                    if (Boolean.TRUE.equals(isUpdated)) {
                        log.info("Rates have already been updated for today.");
                        return Mono.empty(); // No need to proceed if rates are already updated
                    } else {
                        log.info("Fetching and updating currency rates for today...");
                        return currencyRateService.fetchAndSaveRates(LocalDate.now());
                    }
                })
                .doOnSuccess(v -> log.info("Currency rates updated successfully"))
                .doOnError(e -> log.error("Failed to update currency rates: {}", e.getMessage()))
                .subscribe();
    }
}