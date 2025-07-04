package ru.utlc.dto.paymenttype;

import ru.utlc.dto.auditinginfo.AuditingInfoDto;

public record PaymentTypeReadDto(
        Integer id,
        String name,
        String description,
        AuditingInfoDto auditingInfoDto
) {
}
