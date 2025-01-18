package ru.utlc.financialmanagementservice.dto.payment;

import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentReadDto(
        Long id,
        Integer clientId,
        Integer paymentTypeId,
        BigDecimal amount,
        BigDecimal processingFees,
        BigDecimal totalAmount,
        BigDecimal unallocatedAmount,
        BigDecimal allocatedAmount,
        Boolean isFullyAllocated,
        Integer currencyId,
        LocalDate paymentDate,
        String commentary,
        AuditingInfoDto auditingInfoDto
) {
}