package ru.utlc.financialmanagementservice.dto.clientstatus;


import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;

public record ClientStatusReadDto(
        Integer id,
        String name,
        AuditingInfoDto auditingInfoDto
) {}


