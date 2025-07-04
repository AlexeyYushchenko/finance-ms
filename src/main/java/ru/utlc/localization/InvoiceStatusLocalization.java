package ru.utlc.localization;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("invoice_status_localization")
public class InvoiceStatusLocalization {

    @Column("invoice_status_id")
    private Integer invoiceStatusId; // Foreign key to InvoiceStatus

    @Column("language_code")
    private String languageCode;

    @Column("localized_name")
    private String localizedName;
}
