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
public record InvoiceDto(
        Long id,
        InvoiceDirection direction,
        Long partnerId,
        Integer serviceTypeId,
        BigDecimal totalAmount,
        BigDecimal outstandingBalance,
        BigDecimal paidAmount,
        Integer currencyId,
        LocalDate issueDate,
        LocalDate dueDate,
        String commentary,
        Long shipmentId,
        Integer statusId,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) implements Auditable {
}
