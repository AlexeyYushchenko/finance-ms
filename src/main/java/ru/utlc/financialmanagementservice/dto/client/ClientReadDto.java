package ru.utlc.financialmanagementservice.dto.client;


import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;
import ru.utlc.financialmanagementservice.dto.businesstype.BusinessTypeReadDto;
import ru.utlc.financialmanagementservice.dto.clientstatus.ClientStatusReadDto;
import ru.utlc.financialmanagementservice.dto.industrytype.IndustryTypeReadDto;

public record ClientReadDto(
        Integer id,
        String name,
        String fullName,
        ClientStatusReadDto clientStatus,
        BusinessTypeReadDto businessType,
        IndustryTypeReadDto industryType,
        String address,
        AuditingInfoDto auditingInfoDto
) {
}
