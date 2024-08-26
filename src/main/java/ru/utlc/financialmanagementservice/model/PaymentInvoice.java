package ru.utlc.financialmanagementservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("payment_invoice")
public class PaymentInvoice {

    @Id
    private Long id;

    private Long paymentId; // Foreign key to Payment entity
    private Long invoiceId; // Foreign key to Invoice entity

    private BigDecimal allocatedAmount; // Original amount in payment's currency
    private BigDecimal convertedAmount; // Amount after conversion to invoice's currency

    private Long currencyFromId; // Foreign key to Currency entity (Payment's currency)
    private Long currencyToId; // Foreign key to Currency entity (Invoice's currency)

    private BigDecimal exchangeRate; // Exchange rate used for conversion
}
