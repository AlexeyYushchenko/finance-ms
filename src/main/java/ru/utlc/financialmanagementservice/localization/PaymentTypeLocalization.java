package ru.utlc.financialmanagementservice.localization;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("payment_type_localization")
public class PaymentTypeLocalization {

    @Column("payment_type_id")
    private Integer paymentTypeId; // Foreign key to PaymentType

    @Column("language_code")
    private String languageCode;

    @Column("localized_name")
    private String localizedName;

    @Column("localized_description")
    private String localizedDescription;
}
