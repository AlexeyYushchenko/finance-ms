package ru.utlc.financialmanagementservice.dto.paymentinvoice;

import ru.utlc.financialmanagementservice.dto.currency.CurrencyReadDto;
import ru.utlc.financialmanagementservice.dto.invoice.InvoiceReadDto;
import ru.utlc.financialmanagementservice.dto.payment.PaymentReadDto;
import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;
import java.math.BigDecimal;

public record PaymentInvoiceReadDto(
        Long id,
        PaymentReadDto payment,
        InvoiceReadDto invoice,
        BigDecimal allocatedAmount,
        BigDecimal convertedAmount,
        CurrencyReadDto currencyFrom,
        CurrencyReadDto currencyTo,
        BigDecimal exchangeRate,
        AuditingInfoDto auditingInfoDto
) {
}
