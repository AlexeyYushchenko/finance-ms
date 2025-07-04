package ru.utlc.model;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("exchange_rate")
public class ExchangeRate extends AuditingEntity<Long> {

    @Id
    private Long id;
    private Integer currencyFromId;
    private Integer currencyToId;
    private BigDecimal officialRate;
    private BigDecimal standardRate;
    private BigDecimal premiumClientRate;
    private LocalDate rateDate;
}
