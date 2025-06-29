package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.utlc.financialmanagementservice.model.LedgerEntry;

import java.math.BigDecimal;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */
@Repository
public interface LedgerEntryRepository extends R2dbcRepository<LedgerEntry, Long> {

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
          FROM transaction_ledger
         WHERE partner_id = :partnerId
           AND currency_id = :currencyId
    """)
    Mono<BigDecimal> sumAmountByPartnerAndCurrency(Long partnerId, Integer currencyId);

    @Query("""
        SELECT COALESCE(SUM(base_amount), 0)
          FROM transaction_ledger
         WHERE partner_id = :partnerId
    """)
    Mono<BigDecimal> sumBaseAmountByPartner(Long partnerId);

    @Query("""
        SELECT *
          FROM transaction_ledger
         WHERE partner_id = :partnerId
           AND reference_type_id = 3
    """)
    Flux<LedgerEntry> findAllocationsByPartnerId(Long partnerId);

    @Query("""
        SELECT *
          FROM transaction_ledger
         WHERE invoice_id = :invoiceId
           AND reference_type_id = 3
    """)
    Flux<LedgerEntry> findAllocationsByInvoiceId(Long invoiceId);

    @Query("""
        SELECT *
          FROM transaction_ledger
         WHERE payment_id = :paymentId
           AND reference_type_id IN (3, 4)
    """)
    Flux<LedgerEntry> findAllocationsByPaymentId(Long paymentId);

    @Query("""
    SELECT *
      FROM transaction_ledger
     WHERE payment_id = :paymentId
       AND invoice_id = :invoiceId
       AND reference_type_id = :referenceTypeId
""")
    Flux<LedgerEntry> findAllByPaymentIdAndInvoiceIdAndReferenceTypeId(Long paymentId, Long invoiceId, Integer referenceTypeId);

}