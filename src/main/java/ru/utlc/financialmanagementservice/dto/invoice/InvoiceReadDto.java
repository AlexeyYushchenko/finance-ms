package ru.utlc.financialmanagementservice.dto.invoice;

import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceReadDto(
        Long id,
        Integer clientId,
        Integer serviceTypeId,
        BigDecimal totalAmount,
        BigDecimal outstandingBalance,
        BigDecimal amountPaid,
        Integer currencyId,
        LocalDate issueDate,
        LocalDate dueDate,
        String commentary,
        Long shipmentId,
        Integer statusId,
        AuditingInfoDto auditingInfoDto
) {
}

