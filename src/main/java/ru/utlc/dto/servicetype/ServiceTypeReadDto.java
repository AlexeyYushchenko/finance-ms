package ru.utlc.dto.servicetype;

import ru.utlc.dto.auditinginfo.AuditingInfoDto;

public record ServiceTypeReadDto(
        Integer id,
        String name,
        String description,
        AuditingInfoDto auditingInfoDto
) {
}
