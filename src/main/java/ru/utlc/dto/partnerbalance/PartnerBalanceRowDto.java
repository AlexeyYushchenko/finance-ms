package ru.utlc.dto.partnerbalance;

import java.math.BigDecimal;

public record PartnerBalanceRowDto(
        Integer currencyId,
        String currencyCode,
        BigDecimal leftover,      // Payment leftover for that currency
        BigDecimal unpaid,        // Sum of fully unpaid invoices
        BigDecimal partiallyPaid, // Sum of partially paid invoice amounts
        BigDecimal outstanding,   // Typically = unpaid + partiallyPaid
        BigDecimal leftoverRub,   // leftover converted to RUB
        BigDecimal outstandingRub // outstanding converted to RUB
) {}
