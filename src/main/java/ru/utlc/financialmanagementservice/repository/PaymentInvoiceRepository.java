package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.model.PaymentInvoice;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */
public interface PaymentInvoiceRepository extends R2dbcRepository<PaymentInvoice, Long>{
    Flux<PaymentInvoice> findAllByInvoiceId(Long invoiceId);
    Flux<PaymentInvoice> findAllByPaymentId(Long paymentId);

    @Query("SELECT payment_invoice.* FROM payment_invoice " +
            "JOIN payment p ON payment_invoice.payment_id = p.id " +
            "WHERE p.client_id = :clientId")
    Flux<PaymentInvoice> findAllByClientId(@Param("clientId") Integer clientId);

    Mono<Boolean> existsByPaymentId(Long paymentId);
    Mono<Boolean> existsByInvoiceId(Long invoiceId);
}
