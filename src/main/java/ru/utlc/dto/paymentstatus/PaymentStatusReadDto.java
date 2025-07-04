package ru.utlc.dto.paymentstatus;


import ru.utlc.dto.auditinginfo.AuditingInfoDto;

public record PaymentStatusReadDto(
        Integer id,
        String name,
        AuditingInfoDto auditingInfoDto
) {
}
