package ru.utlc.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import ru.utlc.model.Payment;
import ru.utlc.model.PaymentLeftoverAggregate;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */
public interface PaymentRepository extends R2dbcRepository<Payment, Long> {

    Flux<Payment> findAllByPartnerId(Long partnerId);

    @Query("""
                SELECT p.currency_id AS "currencyId",
                       SUM(p.unallocated_amount) AS "leftoverSum"
                  FROM payment p
                 WHERE p.partner_id = :partnerId
                   AND p.payment_status_id = 1  -- only 'completed' are accounted
                 GROUP BY p.currency_id
            """)
    Flux<PaymentLeftoverAggregate> sumLeftoverByPartner(Long partnerId);


}