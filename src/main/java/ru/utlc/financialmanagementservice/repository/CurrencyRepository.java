package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.model.Currency;

import java.util.List;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */
public interface CurrencyRepository extends R2dbcRepository<Currency, Integer> {
    Flux<Currency> findByEnabledTrue();

    @Query("DELETE FROM currency WHERE code IN (:codes)")
    Mono<Void> deleteAllByCodeIn(List<String> codes);

    Mono<Currency> findByCode (String code);

}