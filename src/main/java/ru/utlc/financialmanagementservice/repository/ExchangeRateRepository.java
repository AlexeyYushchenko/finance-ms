package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.model.ExchangeRate;

import java.time.LocalDate;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */
public interface ExchangeRateRepository extends R2dbcRepository<ExchangeRate, Long>, ExchangeRateRepositoryCustom {
    Mono<ExchangeRate> findByCurrencyFromIdAndCurrencyToIdAndRateDate(Integer currencyFromId, Integer currencyToId, LocalDate rateDate);

    Mono<ExchangeRate> findExchangeRateByCurrencyFromIdAndRateDate(Integer currencyFromId, LocalDate rateDate);

    @Query("SELECT e FROM exchange_rate e WHERE e.rate_date = :date")
    Flux<ExchangeRate> findAnyRateByDate(@Param("date") LocalDate date);

}
