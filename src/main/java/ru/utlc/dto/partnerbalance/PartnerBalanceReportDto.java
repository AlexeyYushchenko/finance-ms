package ru.utlc.dto.partnerbalance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PartnerBalanceReportDto(
    Long partnerId,
    LocalDate reportDate,
    List<PartnerBalanceRowDto> rows,
    // Totals across all rows in rubles
    BigDecimal totalLeftoverRub,
    BigDecimal totalOutstandingRub
) {}
