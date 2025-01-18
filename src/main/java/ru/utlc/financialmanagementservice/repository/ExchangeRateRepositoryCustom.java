package ru.utlc.financialmanagementservice.repository;

import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.model.ExchangeRate;

public interface ExchangeRateRepositoryCustom {

    /**
     * Performs an upsert (insert or update) for the given ExchangeRate 
     * based on the unique (currency_from_id, currency_to_id, rate_date).
     */
    Mono<ExchangeRate> upsertExchangeRate(ExchangeRate exchangeRate);
}
