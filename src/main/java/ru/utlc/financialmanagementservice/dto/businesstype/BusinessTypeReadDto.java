package ru.utlc.financialmanagementservice.dto.businesstype;


import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;

public record BusinessTypeReadDto(
        Integer id,
        String name,
        String description,
        AuditingInfoDto auditingInfoDto
) {
}