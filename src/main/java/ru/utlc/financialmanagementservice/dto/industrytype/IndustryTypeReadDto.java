package ru.utlc.financialmanagementservice.dto.industrytype;


import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;

public record IndustryTypeReadDto(
        Integer id,
        String name,
        String description,
        AuditingInfoDto auditingInfoDto
) {
}