//package ru.utlc.financialmanagementservice.dto.paymentinvoice;
//
//import jakarta.validation.constraints.*;
//import lombok.Builder;
//
//import java.math.BigDecimal;
//
//@Builder(toBuilder = true)
//public record PaymentInvoiceCreateDto(
//        @NotNull(message = "{validation.paymentInvoice.paymentId.required}")
//        Long paymentId,
//
//        @NotNull(message = "{validation.paymentInvoice.invoiceId.required}")
//        Long invoiceId,
//
//        @NotNull(message = "{validation.paymentInvoice.allocatedAmount.required}")
//        @DecimalMin(value = "0.01", message = "{validation.paymentInvoice.allocatedAmount.min}")
//        @Digits(integer = 10, fraction = 2, message = "{validation.paymentInvoice.allocatedAmount.format}")
//        BigDecimal allocatedAmount
//) {
//}
