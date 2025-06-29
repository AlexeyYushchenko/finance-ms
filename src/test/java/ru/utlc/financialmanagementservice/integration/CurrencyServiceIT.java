package ru.utlc.financialmanagementservice.integration;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.utlc.financialmanagementservice.dto.currency.CurrencyCreateUpdateDto;
import ru.utlc.financialmanagementservice.dto.currency.CurrencyReadDto;
import ru.utlc.financialmanagementservice.exception.CurrencyNotFoundException;
import ru.utlc.financialmanagementservice.model.Currency;
import ru.utlc.financialmanagementservice.repository.CurrencyRepository;
import ru.utlc.financialmanagementservice.service.CurrencyService;
import ru.utlc.financialmanagementservice.service.ExchangeRateService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(
        properties = {
                "spring.task.scheduling.enabled=false",
                "spring.main.allow-bean-definition-overriding=true"
        }
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RequiredArgsConstructor
class CurrencyServiceIT extends IntegrationTestBase {

    /* ───── 1. Replace the real ExchangeRateService with a stub ───── */
    @TestConfiguration(proxyBeanMethods = false)
    static class MockExchangeRateConfig {
        @Bean(name = "exchangeRateService")
        @Primary
        ExchangeRateService mockedExchangeRateService() {
            ExchangeRateService mock = Mockito.mock(ExchangeRateService.class);
            Mockito.when(mock.fetchAndSaveRatesForPastYear(anyInt()))
                    .thenReturn(Mono.empty());     // no-op
            return mock;
        }
    }

    /* ───── 2. Wiring ───── */
    @Autowired private CurrencyService    currencyService;
    @Autowired private CurrencyRepository currencyRepository;
    @Autowired private CacheManager       cacheManager;

    /* ───── 3. Helpers ───── */
    private static Currency cur(String c, String okv, String n) {
        return Currency.builder().code(c).okvCode(okv).name(n).enabled(true).build();
    }
    private static final List<Currency> TEST_ROWS = List.of(
            cur("GBP", "826", "British Pound"),
            cur("AUD", "036", "Australian Dollar")
    );

    /* ───── 4. Seed test currencies before each method ───── */
    @BeforeEach
    void seedTestCurrencies() {
        Flux.fromIterable(TEST_ROWS)
                .flatMap(row ->
                        currencyRepository.findByCode(row.getCode())
                                .switchIfEmpty(currencyRepository.save(row)))
                .then()
                .block();
    }

    /* ───── 5. Tests ───── */

    @Test
    void findAll_ShouldReturnAllCurrencies() {
        StepVerifier.create(currencyService.findAll())
                .expectNextCount(6)              // 4 base + GBP + AUD
                .verifyComplete();
    }

    @Test
    void findById_ShouldReturnCurrency() {
        var id = currencyRepository.findByCode("GBP").map(Currency::getId).block();
        assertNotNull(id);

        StepVerifier.create(currencyService.findById(id))
                .assertNext(dto -> assertEquals("GBP", dto.code()))
                .verifyComplete();

        assertNotNull(cacheManager.getCache("currencies").get(id));
    }

    @Test
    void findById_ShouldThrowWhenMissing() {
        StepVerifier.create(currencyService.findById(999))
                .expectError(CurrencyNotFoundException.class)
                .verify();
    }

    @Test
    void create_ShouldAddNewCurrency() {
        CurrencyCreateUpdateDto dto =
                new CurrencyCreateUpdateDto("CAD", "124", "Canadian Dollar", true);

        StepVerifier.create(currencyService.create(dto))
                .assertNext(c -> {
                    assertEquals("CAD", c.code());
                    assertNotNull(c.id());
                    assertNotNull(cacheManager.getCache("currencies").get(c.id()));
                })
                .verifyComplete();
    }

    @Test
    void update_ShouldModifyExistingCurrency() {
        Currency gbp = currencyRepository.findByCode("GBP").block();
        assertNotNull(gbp);

        CurrencyCreateUpdateDto upd =
                new CurrencyCreateUpdateDto("GBP", "826", "British Pound Updated", true);

        StepVerifier.create(currencyService.update(gbp.getId(), upd))
                .assertNext(c -> assertEquals("British Pound Updated", c.name()))
                .verifyComplete();

        CurrencyReadDto cached =
                (CurrencyReadDto) cacheManager.getCache("currencies").get(gbp.getId()).get();
        assertEquals("British Pound Updated", cached.name());
    }

    @Test
    void delete_ShouldRemoveCurrencyAndEvictCache() {
        Currency aud = currencyRepository.findByCode("AUD").block();
        assertNotNull(aud);

        StepVerifier.create(currencyService.delete(aud.getId()))
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(currencyRepository.findById(aud.getId()))
                .verifyComplete();
        assertNull(cacheManager.getCache("currencies").get(aud.getId()));

        // Re-insert AUD so later tests (if any) still see six rows
        currencyRepository.save(cur("AUD", "036", "Australian Dollar")).block();
    }

    @Test
    void getEnabledCurrencies_ShouldReturnEnabledOnly() {
        StepVerifier.create(currencyService.getEnabledCurrencies())
                .expectNextCount(5)              // 3 base enabled + GBP + AUD
                .verifyComplete();
    }
}