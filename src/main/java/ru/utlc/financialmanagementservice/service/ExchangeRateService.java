package ru.utlc.financialmanagementservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.model.ExchangeRate;
import ru.utlc.financialmanagementservice.repository.ExchangeRateRepository;
import ru.utlc.financialmanagementservice.repository.RateUpdateLogRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalField;
import java.time.temporal.TemporalUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final RateUpdateLogRepository rateUpdateLogRepository;

    public Mono<ExchangeRate> findByCurrencyFromIdAndCurrencyToIdAndRateDate(Integer currencyFromId, Integer currencyToId, LocalDate exchangeRateDate) {
        return exchangeRateRepository.findByCurrencyFromIdAndCurrencyToIdAndRateDate(currencyFromId, currencyToId, exchangeRateDate)
                .switchIfEmpty(Mono.empty());
    }

    public Mono<BigDecimal> getExchangeRate(Integer currencyFromId, Integer currencyToId, LocalDate exchangeRateDate) {
        return exchangeRateRepository.findByCurrencyFromIdAndCurrencyToIdAndRateDate(currencyFromId, currencyToId, exchangeRateDate)
                .map(ExchangeRate::getStandardRate); // Or choose the rate type based on client type (standard, premium, etc.)
    }

    public Mono<ExchangeRate> saveExchangeRate(ExchangeRate exchangeRate) {
        return exchangeRateRepository.upsertExchangeRate(exchangeRate);
    }

    public Mono<Void> logSuccessfulRateUpdate(LocalDate date) {
        return rateUpdateLogRepository.saveLogForToday(date)
                .then(Mono.empty());
    }
}