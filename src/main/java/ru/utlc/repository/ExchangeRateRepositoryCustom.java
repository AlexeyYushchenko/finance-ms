package ru.utlc.repository;

import reactor.core.publisher.Mono;
import ru.utlc.model.ExchangeRate;

public interface ExchangeRateRepositoryCustom {

    /**
     * Performs an upsert (insert or update) for the given ExchangeRate 
     * based on the unique (currency_from_id, currency_to_id, rate_date).
     */
    Mono<Void> upsertExchangeRate(ExchangeRate exchangeRate);
}
