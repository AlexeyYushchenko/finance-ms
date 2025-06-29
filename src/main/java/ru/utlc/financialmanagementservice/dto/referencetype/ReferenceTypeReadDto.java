package ru.utlc.financialmanagementservice.dto.referencetype;

import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;

public record ReferenceTypeReadDto(
        Integer id,
        String name,
        AuditingInfoDto auditingInfoDto
) {
}
