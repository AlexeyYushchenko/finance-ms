package ru.utlc.financialmanagementservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Builder
@Table("payment_type")
public class PaymentType extends AuditingEntity<Integer> {

    @Id
    private Integer id;

    @Column("name")
    private String name;

    @Column("description")
    private String description;

    // The localizations are handled manually in a separate table and service
    // You could have a service method to fetch and handle localizations
}
