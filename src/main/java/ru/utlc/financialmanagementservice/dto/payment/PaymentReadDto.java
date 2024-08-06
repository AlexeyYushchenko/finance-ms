package ru.utlc.financialmanagementservice.dto.payment;

import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;
import ru.utlc.financialmanagementservice.dto.currency.CurrencyReadDto;
import ru.utlc.financialmanagementservice.dto.paymenttype.PaymentTypeReadDto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentReadDto(
        Long id,
        Integer clientId,
        PaymentTypeReadDto paymentType,
        BigDecimal amount,
        CurrencyReadDto currency,
        LocalDate paymentDate,
        BigDecimal paymentProcessingFees,
        BigDecimal totalAmount,
        String commentary,
        AuditingInfoDto auditingInfoDto
) {
}