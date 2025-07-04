package ru.utlc.dto.payment;

import ru.utlc.dto.auditinginfo.AuditingInfoDto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentReadDto(
        Long id,
        Integer paymentStatusId,
        Long partnerId,
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