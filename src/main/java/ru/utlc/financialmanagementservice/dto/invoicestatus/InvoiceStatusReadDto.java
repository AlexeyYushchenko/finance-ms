package ru.utlc.financialmanagementservice.dto.invoicestatus;

import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;
import ru.utlc.financialmanagementservice.localization.InvoiceStatusLocalization;

import java.util.Map;

public record InvoiceStatusReadDto(
        Integer id,
        String name,
        AuditingInfoDto auditingInfoDto
) {}