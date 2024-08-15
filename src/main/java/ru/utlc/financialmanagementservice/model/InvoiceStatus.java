package ru.utlc.financialmanagementservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("invoice_status")
public class InvoiceStatus extends AuditingEntity<Integer> {

    @Id
    private Integer id;
    private String name;
}