package ru.utlc.financialmanagementservice.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.utlc.financialmanagementservice.model.ExchangeRate;
import ru.utlc.financialmanagementservice.repository.ExchangeRateRepository;
import ru.utlc.financialmanagementservice.service.ExchangeRateService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates testing reciprocal exchange rates.
 * If 1 USD = 100 RUB, then the stored USD->RUB should be 100, and RUB->USD should be 0.01, etc.
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
public class ReciprocalRatesIT extends IntegrationTestBase {

    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private ExchangeRateRepository exchangeRateRepository;

    @Autowired
    private ExchangeRateService exchangeRateService;

    // Example currency IDs for your DB:
    private static final int RUB = 1; // Ruble
    private static final int USD = 2; // US Dollar
    private static final int EUR = 3; // Euro
    private static final int CNY = 4; // Chinese Yuan

    private static final LocalDate TEST_DATE = LocalDate.of(2025, 1, 1);

    @BeforeEach
    void resetDatabase() {
        databaseClient.sql("TRUNCATE TABLE exchange_rate RESTART IDENTITY CASCADE").fetch().rowsUpdated().block();
    }

    /**
     * Helper to save (upsert) an ExchangeRate with given official/standard/premium rates.
     */
    private Mono<Void> saveRate(int fromId,
                                        int toId,
                                        LocalDate date,
                                        BigDecimal officialRate,
                                        BigDecimal standardRate,
                                        BigDecimal premiumRate) {
        ExchangeRate rate = new ExchangeRate();
        rate.setCurrencyFromId(fromId);
        rate.setCurrencyToId(toId);
        rate.setRateDate(date);
        rate.setOfficialRate(officialRate);
        rate.setStandardRate(standardRate);
        rate.setPremiumClientRate(premiumRate);
        return exchangeRateRepository.upsertExchangeRate(rate);
    }

    /**
     * If 1 USD = 100 RUB, then:
     *  - USD->RUB standard_rate = 100
     *  - RUB->USD standard_rate = 0.01
     */
    @Test
    @DisplayName("Test USD↔RUB reciprocal: 1 USD = 100 RUB => USD->RUB=100, RUB->USD=0.01")
    void testUsdRubReciprocal() {
        // 1) Insert direct rate: USD->RUB = 100
        BigDecimal usdRubRate = BigDecimal.valueOf(100.0);
        saveRate(
                USD, RUB,
                TEST_DATE,
                usdRubRate.subtract(BigDecimal.valueOf(0.5)), // official
                usdRubRate, // standard
                usdRubRate.subtract(BigDecimal.valueOf(0.2))  // premium
        ).block();

        // 2) Insert reciprocal: RUB->USD = 0.01
        BigDecimal rubUsdRate = BigDecimal.valueOf(0.01);
        saveRate(
                RUB, USD,
                TEST_DATE,
                rubUsdRate.subtract(BigDecimal.valueOf(0.0001)), // official
                rubUsdRate, // standard
                rubUsdRate.subtract(BigDecimal.valueOf(0.00005)) // premium
        ).block();

        // 3) Retrieve and verify USD->RUB
        Mono<BigDecimal> usdRubMono = exchangeRateService.getExchangeRate(USD, RUB, TEST_DATE);
        StepVerifier.create(usdRubMono)
                .expectNextMatches(rate -> {
                    assertEquals(0, usdRubRate.compareTo(rate), "USD->RUB should be 100.0");
                    return true;
                })
                .verifyComplete();

        // 4) Retrieve and verify RUB->USD
        Mono<BigDecimal> rubUsdMono = exchangeRateService.getExchangeRate(RUB, USD, TEST_DATE);
        StepVerifier.create(rubUsdMono)
                .expectNextMatches(rate -> {
                    assertEquals(0, rubUsdRate.compareTo(rate), "RUB->USD should be 0.01");
                    return true;
                })
                .verifyComplete();
    }

    /**
     * If 1 EUR = 110 RUB => EUR->RUB=110, RUB->EUR=1/110=0.0090909...
     */
    @Test
    @DisplayName("Test EUR↔RUB reciprocal")
    void testEurRubReciprocal() {
        BigDecimal eurRub = BigDecimal.valueOf(110.0);
        BigDecimal rubEur = BigDecimal.valueOf(0.0090909); // ~1/110 (rounded as desired)

        // Insert direct (EUR->RUB)
        saveRate(
                EUR, RUB, TEST_DATE,
                eurRub,  // official
                eurRub,  // standard
                eurRub   // premium, or adjusted
        ).block();

        // Insert reciprocal (RUB->EUR)
        saveRate(
                RUB, EUR, TEST_DATE,
                rubEur,  // official
                rubEur,  // standard
                rubEur   // premium
        ).block();

        // Verify EUR->RUB
        StepVerifier.create(exchangeRateService.getExchangeRate(EUR, RUB, TEST_DATE))
                .assertNext(rate -> assertEquals(0, eurRub.compareTo(rate), "EUR->RUB is 110"))
                .verifyComplete();

        // Verify RUB->EUR
        StepVerifier.create(exchangeRateService.getExchangeRate(RUB, EUR, TEST_DATE))
                .assertNext(actual -> {
                    BigDecimal expected = BigDecimal.valueOf(0.0090909);
                    BigDecimal difference = actual.subtract(expected).abs();
                    // allow ±0.0000001 difference
                    assertTrue(difference.compareTo(BigDecimal.valueOf(1e-7)) <= 0,
                            "RUB->EUR is ~0.0090909 (±1e-7). Actual: " + actual);
                })
                .verifyComplete();

    }

    @Test
    @DisplayName("Test USD↔EUR reciprocal")
    void testUsdEurReciprocal2() {
        // To achieve USD->EUR = 0.9, we choose:
        // USD->RUB = 90, EUR->RUB = 100.
        BigDecimal usdToRub = BigDecimal.valueOf(90);
        BigDecimal eurToRub = BigDecimal.valueOf(100);
        BigDecimal expectedUsdEur = usdToRub.divide(eurToRub, 6, RoundingMode.HALF_UP); // 0.9
        BigDecimal expectedEurUsd = eurToRub.divide(usdToRub, 6, RoundingMode.HALF_UP); // ~1.111111

        // Insert USD->RUB and EUR->RUB rates
        saveRate(USD, RUB, TEST_DATE, usdToRub, usdToRub, usdToRub).block();
        saveRate(EUR, RUB, TEST_DATE, eurToRub, eurToRub, eurToRub).block();

        // Check USD->EUR
        StepVerifier.create(exchangeRateService.getExchangeRate(USD, EUR, TEST_DATE))
                .assertNext(rate -> assertEquals(0, expectedUsdEur.compareTo(rate),
                        "USD->EUR should be " + expectedUsdEur))
                .verifyComplete();

        // Check EUR->USD
        StepVerifier.create(exchangeRateService.getExchangeRate(EUR, USD, TEST_DATE))
                .assertNext(rate -> assertEquals(0, expectedEurUsd.compareTo(rate),
                        "EUR->USD should be " + expectedEurUsd))
                .verifyComplete();
    }

    @Test
    @DisplayName("Test USD↔CNY reciprocal")
    void testUsdCnyReciprocal() {
        // For USD->CNY = 6.5, choose:
        // USD->RUB = 65, CNY->RUB = 10.
        BigDecimal usdToRub = BigDecimal.valueOf(65);
        BigDecimal cnyToRub = BigDecimal.valueOf(10);
        BigDecimal expectedUsdCny = usdToRub.divide(cnyToRub, 6, RoundingMode.HALF_UP); // 6.5
        BigDecimal expectedCnyUsd = cnyToRub.divide(usdToRub, 6, RoundingMode.HALF_UP); // ~0.153846

        // Insert USD->RUB and CNY->RUB rates
        saveRate(USD, RUB, TEST_DATE, usdToRub, usdToRub, usdToRub).block();
        saveRate(CNY, RUB, TEST_DATE, cnyToRub, cnyToRub, cnyToRub).block();

        // Check USD->CNY
        StepVerifier.create(exchangeRateService.getExchangeRate(USD, CNY, TEST_DATE))
                .assertNext(rate -> assertEquals(0, expectedUsdCny.compareTo(rate),
                        "USD->CNY should be " + expectedUsdCny))
                .verifyComplete();

        // Check CNY->USD
        StepVerifier.create(exchangeRateService.getExchangeRate(CNY, USD, TEST_DATE))
                .assertNext(rate -> assertEquals(0, expectedCnyUsd.compareTo(rate),
                        "CNY->USD should be " + expectedCnyUsd))
                .verifyComplete();
    }

    @Test
    @DisplayName("Test EUR↔CNY reciprocal")
    void testEurCnyReciprocal() {
        // For EUR->CNY = 7.0, choose:
        // EUR->RUB = 70, CNY->RUB = 10.
        BigDecimal eurToRub = BigDecimal.valueOf(70);
        BigDecimal cnyToRub = BigDecimal.valueOf(10);
        BigDecimal expectedEurCny = eurToRub.divide(cnyToRub, 6, RoundingMode.HALF_UP); // 7.0
        BigDecimal expectedCnyEur = cnyToRub.divide(eurToRub, 6, RoundingMode.HALF_UP); // ~0.142857

        // Insert EUR->RUB and CNY->RUB rates
        saveRate(EUR, RUB, TEST_DATE, eurToRub, eurToRub, eurToRub).block();
        saveRate(CNY, RUB, TEST_DATE, cnyToRub, cnyToRub, cnyToRub).block();

        // Check EUR->CNY
        StepVerifier.create(exchangeRateService.getExchangeRate(EUR, CNY, TEST_DATE))
                .assertNext(rate -> assertEquals(0, expectedEurCny.compareTo(rate),
                        "EUR->CNY should be " + expectedEurCny))
                .verifyComplete();

        // Check CNY->EUR
        StepVerifier.create(exchangeRateService.getExchangeRate(CNY, EUR, TEST_DATE))
                .assertNext(rate -> assertEquals(0, expectedCnyEur.compareTo(rate),
                        "CNY->EUR should be " + expectedCnyEur))
                .verifyComplete();
    }





    /**
     * If 1 RUB = 0.06 CNY => RUB->CNY=0.06, CNY->RUB=16.6666...
     */
    @Test
    @DisplayName("Test RUB↔CNY reciprocal")
    void testRubCnyReciprocal() {
        BigDecimal rubCny = BigDecimal.valueOf(0.06);
        BigDecimal cnyRub = BigDecimal.valueOf(16.666667).setScale(6, BigDecimal.ROUND_HALF_UP);
        // ~1 / 0.06 = 16.6666...

        saveRate(RUB, CNY, TEST_DATE, rubCny, rubCny, rubCny).block();
        saveRate(CNY, RUB, TEST_DATE, cnyRub, cnyRub, cnyRub).block();

        // Check RUB->CNY
        StepVerifier.create(exchangeRateService.getExchangeRate(RUB, CNY, TEST_DATE))
                .assertNext(rate -> assertEquals(0, rubCny.compareTo(rate), "RUB->CNY=0.06"))
                .verifyComplete();

        // Check CNY->RUB
        StepVerifier.create(exchangeRateService.getExchangeRate(CNY, RUB, TEST_DATE))
                .assertNext(rate -> {
                    assertEquals(0, cnyRub.compareTo(rate), "CNY->RUB=16.666667");
                })
                .verifyComplete();
    }
}
