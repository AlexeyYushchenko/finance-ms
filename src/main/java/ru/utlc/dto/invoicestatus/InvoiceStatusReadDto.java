package ru.utlc.dto.invoicestatus;

import ru.utlc.dto.auditinginfo.AuditingInfoDto;

public record InvoiceStatusReadDto(
        Integer id,
        String name,
        AuditingInfoDto auditingInfoDto
) {}