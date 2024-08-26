package ru.utlc.financialmanagementservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
/*
 * Copyright (c) 2024, ООО Ю-ТЛК МОСКВА. All rights reserved.
 * Licensed under Proprietary License.
 *
 * Author: Алексей Ющенко, ООО Ю-ТЛК МОСКВА
 * Date: 2024-08-19
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("payment")
public class Payment extends AuditingEntity<Long> {

    @Id
    private Long id;
    private Integer clientId;
    private BigDecimal amount;
    private Long currencyId;
    private LocalDate paymentDate;
    private Long paymentTypeId;
    private BigDecimal processingFees;
    private BigDecimal totalAmount;
    private String commentary;

    public void calculateTotalAmount() {
        if (processingFees != null) {
            this.totalAmount = this.amount.subtract(processingFees);
        } else {
            this.totalAmount = this.amount;
        }
    }
}
