package ru.utlc.financialmanagementservice.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("invoice")  // R2DBC Table annotation
public class Invoice extends AuditingEntity<Long> {

    @Id
    private Long id;  // ID generation handled by the database

    @Column("client_id")
    private Integer clientId;

    // Relationships must be handled manually
    @Column("service_type_id")
    private Long serviceTypeId;  // Store as a foreign key reference

    @Column("total_amount")
    private BigDecimal totalAmount;

    @Column("currency_id")
    private Long currencyId;  // Store as a foreign key reference

    @Column("issue_date")
    private LocalDate issueDate;

    @Column("due_date")
    private LocalDate dueDate;

    @Column("commentary")
    private String commentary;

    @Column("shipment_id")
    private Long shipmentId;

    @Column("status_id")
    private Long statusId;  // Store as a foreign key reference
}
