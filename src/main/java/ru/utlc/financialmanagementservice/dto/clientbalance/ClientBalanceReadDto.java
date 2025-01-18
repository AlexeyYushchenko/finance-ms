package ru.utlc.financialmanagementservice.dto.clientbalance;

import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;

import java.math.BigDecimal;

public record ClientBalanceReadDto(
        Long id,
        Integer clientId,
        Integer currencyId,
        BigDecimal balance,
        AuditingInfoDto auditingInfoDto
) {
}
