package ru.utlc.financialmanagementservice.localization;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class PaymentTypeLocalization {
    private String localizedName;
    private String localizedDescription;
}
