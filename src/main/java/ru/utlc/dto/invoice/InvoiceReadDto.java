package ru.utlc.dto.invoice;

import ru.utlc.dto.auditinginfo.AuditingInfoDto;
import ru.utlc.model.InvoiceDirection;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceReadDto(
        Long id,
        InvoiceDirection direction,
        Long partnerId,
        Integer serviceTypeId,
        BigDecimal totalAmount,
        BigDecimal outstandingBalance,
        BigDecimal paidAmount,
        Integer currencyId,
        LocalDate issueDate,
        LocalDate dueDate,
        String commentary,
        Long shipmentId,
        Integer statusId,
        AuditingInfoDto auditingInfoDto
) {
}

