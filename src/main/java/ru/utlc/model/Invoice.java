package ru.utlc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
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
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("invoice")
public class Invoice extends AuditingEntity<Long> {

    @Id
    private Long id;

    private InvoiceDirection direction;
    private Long partnerId;
    private Integer serviceTypeId;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private Integer currencyId;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private String commentary;
    private Long shipmentId;
    private Integer statusId;
    @Version
    private Long version;

    public BigDecimal getOutstandingBalance() {
        return totalAmount.subtract(paidAmount != null ? paidAmount : BigDecimal.ZERO);
    }
}
