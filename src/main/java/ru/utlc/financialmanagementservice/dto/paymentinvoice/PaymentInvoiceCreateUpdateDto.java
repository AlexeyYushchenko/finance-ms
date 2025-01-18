package ru.utlc.financialmanagementservice.dto.paymentinvoice;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.math.BigDecimal;

@Builder(toBuilder = true)
public record PaymentInvoiceCreateUpdateDto(
        @NotNull(message = "{validation.paymentInvoice.paymentId.required}")
        Long paymentId,

        @NotNull(message = "{validation.paymentInvoice.invoiceId.required}")
        Long invoiceId,

        @NotNull(message = "{validation.paymentInvoice.allocatedAmount.required}")
        @DecimalMin(value = "0.01", message = "{validation.paymentInvoice.allocatedAmount.min}")
        @Digits(integer = 10, fraction = 2, message = "{validation.paymentInvoice.allocatedAmount.format}")
        BigDecimal allocatedAmount,

        @NotNull(message = "{validation.paymentInvoice.convertedAmount.required}")
        @DecimalMin(value = "0.01", message = "{validation.paymentInvoice.convertedAmount.min}")
        @Digits(integer = 10, fraction = 2, message = "{validation.paymentInvoice.convertedAmount.format}")
        BigDecimal convertedAmount,

        @NotNull(message = "{validation.paymentInvoice.currencyFromId.required}")
        Integer currencyFromId,

        @NotNull(message = "{validation.paymentInvoice.currencyToId.required}")
        Integer currencyToId,

        @NotNull(message = "{validation.paymentInvoice.exchangeRate.required}")
        @DecimalMin(value = "0.000001", message = "{validation.paymentInvoice.exchangeRate.min}")
        @Digits(integer = 10, fraction = 6, message = "{validation.paymentInvoice.exchangeRate.format}")
        BigDecimal exchangeRate
) {
}
