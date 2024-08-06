package ru.utlc.financialmanagementservice.dto.servicetype;

import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;
import ru.utlc.financialmanagementservice.localization.ServiceTypeLocalization;

import java.util.Map;

public record ServiceTypeReadDto(
        Integer id,
        String name,
        String description,
        Map<String, ServiceTypeLocalization> localizations,
        AuditingInfoDto auditingInfoDto
) {
}
