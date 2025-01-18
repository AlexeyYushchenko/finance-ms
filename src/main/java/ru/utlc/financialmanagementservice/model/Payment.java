package ru.utlc.financialmanagementservice.model;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
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
@Slf4j
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
    private LocalDate paymentDate;
    private Integer paymentTypeId;
    private Integer currencyId;
    private BigDecimal amount;
    private BigDecimal processingFees;
    private BigDecimal totalAmount;
    private BigDecimal unallocatedAmount;
    @Transient private BigDecimal allocatedAmount;
    @Transient private Boolean isFullyAllocated;
    private String commentary;
    @Version
    private Long version;

    public Payment calculateTotalAmount() {
        if (processingFees != null) {
            this.totalAmount = this.amount.subtract(processingFees);
        } else {
            this.totalAmount = this.amount;
        }
        return this;
    }

    public Payment(Integer clientId, Integer paymentTypeId, BigDecimal amount, Integer currencyId, LocalDate paymentDate, BigDecimal processingFees, String commentary) {
        this.clientId = clientId;
        this.paymentTypeId = paymentTypeId;
        this.amount = amount;
        this.currencyId = currencyId;
        this.paymentDate = paymentDate;
        this.processingFees = processingFees != null ? processingFees : BigDecimal.ZERO;
        this.commentary = commentary;
    }
}
