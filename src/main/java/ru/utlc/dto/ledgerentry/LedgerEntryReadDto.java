package ru.utlc.dto.ledgerentry;

import ru.utlc.dto.auditinginfo.AuditingInfoDto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LedgerEntryReadDto(
        Long id,
        Long partnerId,
        Integer currencyId,
        BigDecimal amount,
        BigDecimal baseAmount,
        Integer referenceTypeId,
        Long invoiceId,
        Long paymentId,
        LocalDate transactionDate,
        AuditingInfoDto auditingInfoDto
) {
}
