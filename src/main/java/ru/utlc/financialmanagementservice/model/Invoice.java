package ru.utlc.financialmanagementservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
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
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("invoice")
public class Invoice extends AuditingEntity<Long> {

    @Id
    private Long id;
    private Integer clientId;
    private Integer serviceTypeId;
    private BigDecimal totalAmount;
    @Transient private BigDecimal amountPaid;
    @Transient private BigDecimal outstandingBalance;
    private Integer currencyId;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private String commentary;
    private Long shipmentId;
    private Integer statusId;
    @Version
    private Long version;
}
