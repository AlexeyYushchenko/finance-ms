package ru.utlc.financialmanagementservice.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("currency")
public class Currency extends AuditingEntity<Integer> {

    @Id
    private Integer id;
    private String code;
    private String name;
    private Boolean enabled;
}
