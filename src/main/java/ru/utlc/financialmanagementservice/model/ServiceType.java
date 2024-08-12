package ru.utlc.financialmanagementservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@EqualsAndHashCode(callSuper=true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "service_type")
public class ServiceType extends AuditingEntity<Integer> {

    @Id
    private Integer id;

    @Column("name")
    private String name;

    @Column("description")
    private String description;
}