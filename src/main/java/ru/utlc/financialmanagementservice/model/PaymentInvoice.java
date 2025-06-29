//package ru.utlc.financialmanagementservice.model;
//
//import lombok.*;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.data.annotation.Id;
//import org.springframework.data.annotation.Version;
//import org.springframework.data.relational.core.mapping.Table;
//
//import java.math.BigDecimal;
//
//@Slf4j
//@Data
//@EqualsAndHashCode(callSuper = true)
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//@Table("payment_invoice")
//public class PaymentInvoice extends AuditingEntity<Long> {
//
//    @Id
//    private Long id;
//
//    @Version
//    private Long version;
//
//    private Long paymentId; // Foreign key to Payment entity
//    private Long invoiceId; // Foreign key to Invoice entity
//
//    private BigDecimal allocatedAmount; // Original amount in payment's currency
//    private BigDecimal convertedAmount; // Amount after conversion to invoice's currency
//
//    private Integer currencyFromId; // Foreign key to Currency entity (Payment's currency)
//    private Integer currencyToId; // Foreign key to Currency entity (Invoice's currency)
//
//    private BigDecimal exchangeRate; // Exchange rate used for conversion
//}
