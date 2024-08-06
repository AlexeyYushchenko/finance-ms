package ru.utlc.financialmanagementservice.dto.currency;

import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;

public record CurrencyReadDto(
        Integer id,
        String code,
        String name,
        Boolean enabled,
        AuditingInfoDto auditingInfoDto
) {
}
