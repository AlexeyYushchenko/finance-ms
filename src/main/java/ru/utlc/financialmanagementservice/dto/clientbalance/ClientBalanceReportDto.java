package ru.utlc.financialmanagementservice.dto.clientbalance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ClientBalanceReportDto(
    Long partnerId,
    LocalDate reportDate,
    List<ClientBalanceRowDto> rows,
    // Totals across all rows in rubles
    BigDecimal totalLeftoverRub,
    BigDecimal totalOutstandingRub
) {}
