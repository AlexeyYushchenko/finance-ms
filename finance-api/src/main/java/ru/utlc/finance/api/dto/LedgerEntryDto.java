package ru.utlc.finance.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
/*
 * Copyright (c) 2024, Aleksey Yushchenko (13yae13@gmail.com). All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Aleksey Yushchenko (13yae13@gmail.com)
 * Date: 2024-08-19
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LedgerEntryDto(
        Long id,
        Long partnerId,
        Integer currencyId,
        BigDecimal amount,
        BigDecimal baseAmount,
        Integer referenceTypeId,
        Long invoiceId,
        Long paymentId,
        LocalDate transactionDate,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) implements Auditable {}
