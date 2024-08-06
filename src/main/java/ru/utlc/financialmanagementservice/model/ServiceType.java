package ru.utlc.financialmanagementservice.model;

import jakarta.persistence.*;
import lombok.*;
import ru.utlc.financialmanagementservice.localization.PaymentTypeLocalization;
import ru.utlc.financialmanagementservice.localization.ServiceTypeLocalization;

import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode(callSuper=true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "service_type")
public class ServiceType extends AuditingEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "service_type_localization", joinColumns = @JoinColumn(name = "service_type_id"))
    @MapKeyColumn(name = "language_code")
    private Map<String, ServiceTypeLocalization> localizations = new HashMap<>();
}