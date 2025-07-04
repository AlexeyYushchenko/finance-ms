package ru.utlc.model;

import org.springframework.data.relational.core.mapping.Column;

import java.math.BigDecimal;
public record InvoiceAggregate(
    @Column("currencyId")  Integer currencyId,
    @Column("totalUnpaid")  BigDecimal totalUnpaid,
    @Column("partiallyPaid")  BigDecimal partiallyPaid,
    @Column("outstanding")  BigDecimal outstanding
) {}