package ru.utlc.financialmanagementservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.utlc.financialmanagementservice.localization.InvoiceStatusLocalization;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invoice_status")
public class InvoiceStatus extends AuditingEntity<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", nullable = false, unique = true)
    private String name;

    @Builder.Default
    @ElementCollection
    @CollectionTable(name = "invoice_status_localization", joinColumns = @JoinColumn(name = "invoice_status_id"))
    @MapKeyColumn(name = "language_code")
    private Map<String, InvoiceStatusLocalization> localizations = new HashMap<>();

    // Optionally, if you want to track the history of routeStatus changes for each invoice
    // @OneToMany(mappedBy = "routeStatus")
    // private List<Invoice> invoices;
}
