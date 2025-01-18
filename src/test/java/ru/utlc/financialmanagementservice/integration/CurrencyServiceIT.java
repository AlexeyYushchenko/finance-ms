package ru.utlc.financialmanagementservice.integration;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import ru.utlc.financialmanagementservice.dto.currency.CurrencyCreateUpdateDto;
import ru.utlc.financialmanagementservice.exception.CurrencyNotFoundException;
import ru.utlc.financialmanagementservice.model.Currency;
import ru.utlc.financialmanagementservice.repository.CurrencyRepository;
import ru.utlc.financialmanagementservice.service.CurrencyService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestExecutionListeners(
        listeners = {
                DependencyInjectionTestExecutionListener.class
        },
        mergeMode = TestExecutionListeners.MergeMode.REPLACE_DEFAULTS
)
@ExtendWith(SpringExtension.class)
@Testcontainers
@ActiveProfiles("test")
@SpringBootTest
@RequiredArgsConstructor
class CurrencyServiceIT extends IntegrationTestBase {

    @Autowired
    private CurrencyService currencyService;

    @Autowired
    private CurrencyRepository currencyRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // Add additional currencies for testing
        currencyRepository.saveAll(List.of(
                Currency.builder().code("GBP").okvCode(826).name("British Pound").enabled(true).build(),
                Currency.builder().code("AUD").okvCode(36).name("Australian Dollar").enabled(true).build()
        )).blockLast();
    }

    @AfterEach
    void tearDown() {
        // Remove test-specific currencies
        currencyRepository.deleteAllByCodeIn(List.of("GBP", "AUD")).block();
    }

    @Test
    void findAll_ShouldReturnAllCurrencies() {
        StepVerifier.create(currencyService.findAll())
                .expectNextCount(6) // Includes the four base currencies and two test-specific currencies
                .verifyComplete();

        assertNotNull(cacheManager.getCache("currencies").get("all"));
    }

    @Test
    void findById_ShouldReturnCurrency() {
        Currency savedCurrency = currencyRepository.findByCode("GBP").block();

        assertNotNull(savedCurrency);

        StepVerifier.create(currencyService.findById(savedCurrency.getId()))
                .assertNext(currency -> assertEquals("GBP", currency.code()))
                .verifyComplete();

        assertNotNull(cacheManager.getCache("currencies").get(savedCurrency.getId()));
    }

    @Test
    void findById_ShouldThrowCurrencyNotFoundException() {
        StepVerifier.create(currencyService.findById(999))
                .expectErrorMatches(throwable -> throwable instanceof CurrencyNotFoundException)
                .verify();
    }

    @Test
    void create_ShouldAddNewCurrency() {
        CurrencyCreateUpdateDto newCurrency = new CurrencyCreateUpdateDto("CAD", "124", "Canadian Dollar", true);

        StepVerifier.create(currencyService.create(newCurrency))
                .assertNext(currency -> {
                    assertEquals("CAD", currency.code());
                    assertNotNull(currency.id());
                    // Validate the individual cache entry
                    assertNotNull(cacheManager.getCache("currencies").get(currency.id()));
                })
                .verifyComplete();

        // Validate that "all" cache is not populated automatically
        assertNull(cacheManager.getCache("currencies").get("all"));
    }


    @Test
    void update_ShouldModifyExistingCurrency() {
        Currency savedCurrency = currencyRepository.findByCode("GBP").block();

        assertNotNull(savedCurrency);

        CurrencyCreateUpdateDto updateDto = new CurrencyCreateUpdateDto("GBP", "826", "British Pound Updated", true);

        StepVerifier.create(currencyService.update(savedCurrency.getId(), updateDto))
                .assertNext(updated -> assertEquals("British Pound Updated", updated.name()))
                .verifyComplete();
    }

    @Test
    void delete_ShouldRemoveCurrency() {
        Currency savedCurrency = currencyRepository.findByCode("GBP").block();

        assertNotNull(savedCurrency);

        StepVerifier.create(currencyService.delete(savedCurrency.getId()))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(currencyRepository.findById(savedCurrency.getId()))
                .verifyComplete();

        assertNull(cacheManager.getCache("currencies").get(savedCurrency.getId()));
    }

    @Test
    void getEnabledCurrencies_ShouldReturnEnabledOnly() {
        StepVerifier.create(currencyService.getEnabledCurrencies())
                .expectNextCount(5) // Includes all enabled currencies (base + test-specific)
                .verifyComplete();
    }
}
