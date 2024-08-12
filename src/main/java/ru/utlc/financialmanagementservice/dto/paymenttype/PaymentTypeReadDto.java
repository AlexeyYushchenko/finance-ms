package ru.utlc.financialmanagementservice.dto.paymenttype;

import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;

public record PaymentTypeReadDto(
        Integer id,
        String name,
        String description,
        AuditingInfoDto auditingInfoDto
) {
}
