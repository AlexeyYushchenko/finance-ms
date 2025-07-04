package ru.utlc.dto.currency;

import ru.utlc.dto.auditinginfo.AuditingInfoDto;

public record CurrencyReadDto(
        Integer id,
        String code,
        String okvCode,
        String name,
        Boolean enabled,
        AuditingInfoDto auditingInfoDto
) {
}
