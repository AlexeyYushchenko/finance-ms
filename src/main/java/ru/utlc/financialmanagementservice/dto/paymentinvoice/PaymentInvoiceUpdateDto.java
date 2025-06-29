//package ru.utlc.financialmanagementservice.dto.paymentinvoice;
//
//import jakarta.validation.constraints.DecimalMin;
//import jakarta.validation.constraints.Digits;
//import jakarta.validation.constraints.NotNull;
//import lombok.Builder;
//
//import java.math.BigDecimal;
//
//@Builder(toBuilder = true)
//public record PaymentInvoiceUpdateDto(
//        @NotNull(message = "{validation.paymentInvoice.allocatedAmount.required}")
//        @DecimalMin(value = "0.01", message = "{validation.paymentInvoice.allocatedAmount.min}")
//        @Digits(integer = 10, fraction = 2, message = "{validation.paymentInvoice.allocatedAmount.format}")
//        BigDecimal allocatedAmount
//) {
//}
