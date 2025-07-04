package ru.utlc.dto.partnerbalance;

import ru.utlc.dto.auditinginfo.AuditingInfoDto;

import java.math.BigDecimal;

public record PartnerBalanceReadDto(
        Long id,
        Integer clientId,
        Integer currencyId,
        BigDecimal balance,
        AuditingInfoDto auditingInfoDto
) {
}
