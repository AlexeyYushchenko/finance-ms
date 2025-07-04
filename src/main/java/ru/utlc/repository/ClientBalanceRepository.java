package ru.utlc.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.utlc.model.ClientBalance;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */
public interface ClientBalanceRepository extends R2dbcRepository<ClientBalance, Long> {
    Mono<ClientBalance> findByClientIdAndCurrencyId(Integer clientId, Integer currencyId);

    Flux<ClientBalance> findAllByClientId(Integer clientId);
}
