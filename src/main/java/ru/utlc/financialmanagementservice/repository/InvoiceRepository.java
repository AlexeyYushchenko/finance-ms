package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import ru.utlc.financialmanagementservice.model.Invoice;
import ru.utlc.financialmanagementservice.model.InvoiceAggregate;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */
public interface InvoiceRepository extends R2dbcRepository<Invoice, Long> {

    Flux<Invoice> findAllByPartnerId(Long partnerId);

    @Query("""
        SELECT i.currency_id AS "currencyId",
               SUM(CASE WHEN i.paid_amount=0 THEN i.total_amount ELSE 0 END) AS "totalUnpaid",
               SUM(CASE WHEN i.paid_amount>0 AND i.paid_amount<i.total_amount
                        THEN (i.total_amount - i.paid_amount)
                        ELSE 0 END) AS "partiallyPaid",
               SUM(i.total_amount - i.paid_amount) AS "outstanding"
          FROM invoice i
         WHERE i.partner_id = :partnerId
           AND i.status_id <> 6  -- exclude canceled or adjust as you see fit
         GROUP BY i.currency_id
    """)
    Flux<InvoiceAggregate> sumInvoicesByPartner(Long partnerId);
}

