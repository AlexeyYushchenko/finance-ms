package ru.utlc.financialmanagementservice.dto.paymentinvoice;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record PaymentInvoiceCreateUpdateDto(
        @NotNull(message = "validation.paymentInvoice.paymentId.required")
        Long paymentId,

        @NotNull(message = "validation.paymentInvoice.invoiceId.required")
        Long invoiceId,

        @DecimalMin(value = "0.00", message = "validation.paymentInvoice.allocatedAmount.min")
        @Digits(integer = 10, fraction = 2, message = "validation.paymentInvoice.allocatedAmount.format")
        BigDecimal allocatedAmount,

        @DecimalMin(value = "0.00", message = "validation.paymentInvoice.convertedAmount.min")
        @Digits(integer = 10, fraction = 2, message = "validation.paymentInvoice.convertedAmount.format")
        BigDecimal convertedAmount,

        @NotNull(message = "validation.paymentInvoice.currencyFromId.required")
        Long currencyFromId,

        @NotNull(message = "validation.paymentInvoice.currencyToId.required")
        Long currencyToId,

        @DecimalMin(value = "0.000001", message = "validation.paymentInvoice.exchangeRate.min")
        @Digits(integer = 10, fraction = 6, message = "validation.paymentInvoice.exchangeRate.format")
        BigDecimal exchangeRate
) {
}
