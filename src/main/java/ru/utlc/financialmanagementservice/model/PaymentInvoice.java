//package ru.utlc.financialmanagementservice.model;
//
//import jakarta.persistence.*;
//import lombok.AllArgsConstructor;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//import java.math.BigDecimal;
//
//@Data
//@EqualsAndHashCode(callSuper = true)
//@NoArgsConstructor
//@AllArgsConstructor
//@Entity
//@Builder
//@Table(name = "payment_invoice")
//public class PaymentInvoice {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "payment_id", nullable = false)
//    private Payment payment;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "invoice_id", nullable = false)
//    private Invoice invoice;
//
//    @Column(name = "allocated_amount", precision = 10, scale = 2)
//    private BigDecimal allocatedAmount;
//
//    @Column(name = "converted_amount", precision = 10, scale = 2)
//    private BigDecimal convertedAmount;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "currency_from_id", nullable = false)
//    private Currency currencyFrom;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "currency_to_id", nullable = false)
//    private Currency currencyTo;
//
//    @Column(name = "exchange_rate", precision = 10, scale = 6)
//    private BigDecimal exchangeRate;
//
//}
//
