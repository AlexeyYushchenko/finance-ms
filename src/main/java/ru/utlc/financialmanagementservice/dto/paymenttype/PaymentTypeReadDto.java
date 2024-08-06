package ru.utlc.financialmanagementservice.dto.paymenttype;

import ru.utlc.financialmanagementservice.dto.auditinginfo.AuditingInfoDto;
import ru.utlc.financialmanagementservice.localization.InvoiceStatusLocalization;
import ru.utlc.financialmanagementservice.localization.PaymentTypeLocalization;

import java.util.Map;

public record PaymentTypeReadDto(
        Integer id,
        String name,
        String description,
        Map<String, PaymentTypeLocalization> localizations,
        AuditingInfoDto auditingInfoDto
) {
}
