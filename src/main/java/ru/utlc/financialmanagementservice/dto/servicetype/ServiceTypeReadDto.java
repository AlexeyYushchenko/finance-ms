package ru.utlc.financialmanagementservice.dto.servicetype;

import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;

public record ServiceTypeReadDto(
        Integer id,
        String name,
        String description,
        AuditingInfoDto auditingInfoDto
) {
}
