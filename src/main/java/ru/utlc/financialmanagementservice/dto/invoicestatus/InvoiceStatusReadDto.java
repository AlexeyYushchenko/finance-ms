package ru.utlc.financialmanagementservice.dto.invoicestatus;

import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;

public record InvoiceStatusReadDto(
        Integer id,
        String name,
        AuditingInfoDto auditingInfoDto
) {}