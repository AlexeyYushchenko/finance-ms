package ru.utlc.financialmanagementservice.dto.paymentstatus;


import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;

public record PaymentStatusReadDto(
        Integer id,
        String name,
        AuditingInfoDto auditingInfoDto
) {
}
