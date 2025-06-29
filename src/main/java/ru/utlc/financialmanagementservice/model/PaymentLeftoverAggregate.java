package ru.utlc.financialmanagementservice.model;

import java.math.BigDecimal;

import org.springframework.data.relational.core.mapping.Column;

public record PaymentLeftoverAggregate(
        @Column("currencyId") Integer currencyId,
        @Column("leftoverSum") BigDecimal leftoverSum
) {}

