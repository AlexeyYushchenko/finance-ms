package ru.utlc.localization;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("service_type_localization")
public class ServiceTypeLocalization {

    @Column("service_type_id")
    private Integer serviceTypeId; // Foreign key to ServiceType

    @Column("language_code")
    private String languageCode;

    @Column("localized_name")
    private String localizedName;

    @Column("localized_description")
    private String localizedDescription;
}
