package ru.utlc.financialmanagementservice.model;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */

/**
 * Represents a single row in transaction_ledger table.
 * Tracks financial movements (+ or -) in a given currency for a particular partner.
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("transaction_ledger")
public class LedgerEntry extends AuditingEntity<Long> {

    @Id
    private Long id;

    private Long partnerId;
    private Integer currencyId;

    private BigDecimal amount;       // + or -
    private BigDecimal baseAmount;   // same amount in base currency (e.g. RUB)

    private Integer referenceTypeId; // e.g. "PAYMENT", "ALLOCATION", etc.
    private Long paymentId;
    private Long invoiceId;

    private LocalDate transactionDate;

    @Version
    private Long version;
}
