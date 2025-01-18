package ru.utlc.financialmanagementservice.dto.payment;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentCreateUpdateDto(
        @NotNull(message = "{validation.payment.clientId.required}")
        Integer clientId,

        @PastOrPresent(message = "{validation.payment.paymentDate.pastOrPresent}")
        @NotNull(message = "{validation.payment.paymentDate.required}")
        LocalDate paymentDate,

        @NotNull(message = "{validation.payment.currency.required}")
        Integer currencyId,

        @NotNull(message = "{validation.payment.amount.required}")
        @DecimalMin(value = "0.01", message = "{validation.payment.amount.minimum}")
        @Digits(integer = 10, fraction = 2, message = "{validation.payment.amount.format}")
        BigDecimal amount,

        @DecimalMin(value = "0.00", message = "{validation.payment.processingFees.min}")
        @Digits(integer = 10, fraction = 2, message = "{validation.payment.processingFees.format}")
        BigDecimal processingFees,

        @NotNull(message = "{validation.payment.paymentType.required}")
        Integer paymentTypeId,

        @Size(max = 255, message = "{validation.payment.commentary.size}")
        String commentary
) {
}
