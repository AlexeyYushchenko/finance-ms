package ru.utlc.financialmanagementservice.dto.paymentinvoice;

import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;

import java.math.BigDecimal;

public record PaymentInvoiceReadDto(
        Long id,
        Long paymentId,
        Long invoiceId,
        BigDecimal allocatedAmount,
        BigDecimal convertedAmount,
        Integer currencyFromId,
        Integer currencyToId,
        BigDecimal exchangeRate,
        AuditingInfoDto auditingInfoDto
) {
}