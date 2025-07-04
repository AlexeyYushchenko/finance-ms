package ru.utlc.dto.referencetype;

import ru.utlc.dto.auditinginfo.AuditingInfoDto;

public record ReferenceTypeReadDto(
        Integer id,
        String name,
        AuditingInfoDto auditingInfoDto
) {
}
