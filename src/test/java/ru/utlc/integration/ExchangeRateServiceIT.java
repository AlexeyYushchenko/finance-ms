package ru.utlc.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.utlc.exception.ExchangeRateRetrievalFailedException;
import ru.utlc.model.ExchangeRate;
import ru.utlc.repository.ExchangeRateRepository;
import ru.utlc.service.ExchangeRateService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for ExchangeRateService after we switched to storing only "foreign->RUB" rows.
 *
 * We verify the new logic that:
 * - If from=RUB and to=FOREIGN, we invert the stored (FOREIGN->RUB).
 * - If from=FOREIGN1 and to=FOREIGN2, we do cross-rate (FOREIGN1->RUB)/(FOREIGN2->RUB).
 * - If from=FOREIGN and to=RUB, we just read the stored rate.
 */
@Slf4j
@ExtendWith(SpringExtension.class)
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@TestExecutionListeners(
        listeners = {
                DependencyInjectionTestExecutionListener.class
        },
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
@RequiredArgsConstructor
@DirtiesContext
public class ExchangeRateServiceIT extends IntegrationTestBase {

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private ExchangeRateService exchangeRateService;

    // Example currency IDs:
    private static final int RUB = 1; // "RUB"
    private static final int USD = 2; // "USD"
    private static final int EUR = 3; // "EUR"
    private static final int CNY = 4; // "CNY"

    private static final LocalDate DATE1 = LocalDate.of(2025, 1, 1);

    @BeforeEach
    void resetDatabase() {
        databaseClient.sql("TRUNCATE TABLE exchange_rate RESTART IDENTITY CASCADE")
                .fetch()
                .rowsUpdated()
                .doFinally(signalType -> log.info("Reset database finished with signal: " + signalType))
                .block();
    }

    /**
     * Helper: Insert a single "foreign->RUB" row with the provided officialRate.
     * (official/premium rates set arbitrarily)
     */
    private Mono<Void> insertForeignToRub(int foreignId, BigDecimal officialRate) {
        ExchangeRate rate = new ExchangeRate();
        rate.setCurrencyFromId(foreignId); // e.g. USD
        rate.setCurrencyToId(RUB);         // always RUB
        rate.setRateDate(DATE1);
        rate.setOfficialRate(officialRate);
        rate.setStandardRate(officialRate);
        rate.setPremiumClientRate(officialRate);
        return exchangeRateRepository.upsertExchangeRate(rate)
                .doFinally(signal -> log.info("Insert for foreignId " + foreignId + " finished with signal: " + signal));
    }


    @Test
    @DisplayName("foreign->RUB stored => direct retrieval matches; RUB->foreign => inversion; foreign->foreign => cross-rate")
    void testForeignRubLogic_AllScenarios() {
        /*
         Suppose we store:
           1) USD->RUB = 100
           2) EUR->RUB = 110
         We do NOT store the reverse or any cross because the new logic does that on the fly.

         Then we expect:
           - getExchangeRate(USD, RUB) = 100
           - getExchangeRate(RUB, USD) = 1/100 = 0.01
           - getExchangeRate(EUR, RUB) = 110
           - getExchangeRate(RUB, EUR) = 1/110 = 0.00909...
           - getExchangeRate(USD, EUR) = (USD->RUB)/(EUR->RUB) = 100/110 = ~0.90909
           - getExchangeRate(EUR, USD) = 110/100 = 1.1
         */

        insertForeignToRub(USD, BigDecimal.valueOf(100)).block();
        insertForeignToRub(EUR, BigDecimal.valueOf(110)).block();

        // 1) USD->RUB = 100
        StepVerifier.create(exchangeRateService.getExchangeRate(USD, RUB, DATE1))
                .assertNext(rate -> assertEquals(0, BigDecimal.valueOf(100).compareTo(rate),
                        "USD->RUB should be exactly 100"))
                .verifyComplete();

        // 2) RUB->USD = 1/100 = 0.01
        StepVerifier.create(exchangeRateService.getExchangeRate(RUB, USD, DATE1))
                .assertNext(actual -> {
                    BigDecimal expected = BigDecimal.valueOf(0.01);
                    BigDecimal diff = actual.subtract(expected).abs();
                    // Allow equality within 1e-7
                    assertTrue(diff.compareTo(BigDecimal.valueOf(1e-7)) <= 0,
                            "RUB->USD should be ~0.01 (±1e-7), got " + actual);
                })
                .verifyComplete();

        // 3) EUR->RUB = 110
        StepVerifier.create(exchangeRateService.getExchangeRate(EUR, RUB, DATE1))
                .assertNext(rate -> assertEquals(0, BigDecimal.valueOf(110).compareTo(rate),
                        "EUR->RUB should be 110"))
                .verifyComplete();

        // 4) RUB->EUR = 1/110 = ~0.00909
        StepVerifier.create(exchangeRateService.getExchangeRate(RUB, EUR, DATE1))
                .assertNext(actual -> {
                    BigDecimal expected = BigDecimal.ONE
                            .divide(BigDecimal.valueOf(110), 6, RoundingMode.HALF_UP);
                    BigDecimal diff = actual.subtract(expected).abs();
                    assertTrue(diff.compareTo(BigDecimal.valueOf(1e-7)) <= 0,
                            "RUB->EUR should be " + expected + ", got " + actual);
                })
                .verifyComplete();

        // 5) USD->EUR = (USD->RUB)/(EUR->RUB) = 100/110 = ~0.909091
        StepVerifier.create(exchangeRateService.getExchangeRate(USD, EUR, DATE1))
                .assertNext(actual -> {
                    BigDecimal expected = BigDecimal.valueOf(100)
                            .divide(BigDecimal.valueOf(110), 6, RoundingMode.HALF_UP); // ~0.909091
                    BigDecimal diff = actual.subtract(expected).abs();
                    assertTrue(diff.compareTo(BigDecimal.valueOf(1e-7)) <= 0,
                            "USD->EUR should be " + expected + ", got " + actual);
                })
                .verifyComplete();

        // 6) EUR->USD = 110/100 = 1.1
        StepVerifier.create(exchangeRateService.getExchangeRate(EUR, USD, DATE1))
                .assertNext(actual -> {
                    BigDecimal expected = BigDecimal.valueOf(110)
                            .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP); // 1.1
                    BigDecimal diff = actual.subtract(expected).abs();
                    assertTrue(diff.compareTo(BigDecimal.valueOf(1e-7)) <= 0,
                            "EUR->USD should be " + expected + ", got " + actual);
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Get rate on non-existent date (e.g. 1950) -> should throw exception if no rate is found")
    void testGetExchangeRateForPastDate_NoData() {
        // Insert only USD->RUB=77 on 2025-01-01
        insertForeignToRub(USD, BigDecimal.valueOf(77)).block();

        // 1950-01-01 => no row in DB for (USD->RUB)
        LocalDate oldDate = LocalDate.of(1950, 1, 1);
        Mono<BigDecimal> rateMono = exchangeRateService.getExchangeRate(USD, RUB, oldDate);

        StepVerifier.create(rateMono)
                .expectErrorMatches(throwable ->
                        throwable instanceof ExchangeRateRetrievalFailedException &&
                                throwable.getMessage().contains("error.exchangeRate.retrievalFailed"))
                .verify();
    }

    @Test
    @DisplayName("Get rate on far future date (2100) -> should throw exception if no rate is found")
    void testGetExchangeRateForFutureDate_NoData() {
        // Insert EUR->RUB=88 for 2025-01-01
        insertForeignToRub(EUR, BigDecimal.valueOf(88)).block();

        // 2100-01-01 => no row in DB for (EUR->RUB)
        LocalDate futureDate = LocalDate.of(2100, 1, 1);
        Mono<BigDecimal> rateMono = exchangeRateService.getExchangeRate(EUR, RUB, futureDate);

        StepVerifier.create(rateMono)
                .expectError(ExchangeRateRetrievalFailedException.class)
                .verify();
    }


    @Test
    @DisplayName("Attempt to parse an invalid date string (e.g. '2024-12-32'), expect DateTimeParseException")
    void testGetExchangeRateWithInvalidDateString() {
        // This scenario only applies if you parse strings somewhere.
        // If your code never parses, you can skip or adapt.
        String invalidDateStr = "2024-12-32";
        assertThrows(DateTimeParseException.class, () -> {
            LocalDate.parse(invalidDateStr);
        });
    }
}
