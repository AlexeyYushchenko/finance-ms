package ru.utlc.financialmanagementservice.dto.payment;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentCreateUpdateDto(
        @NotNull(message = "validation.payment.clientId.required")
        Integer clientId,

        @NotNull(message = "validation.payment.amount.required")
        @DecimalMin(value = "0.01", message = "validation.payment.amount.minimum")
        @Digits(integer=10, fraction=2, message = "validation.payment.amount.format")
        BigDecimal amount,

        @NotNull(message = "validation.payment.currency.required")
        Integer currencyId,

        @NotNull(message = "validation.payment.paymentDate.required")
        LocalDate paymentDate,

        @NotNull(message = "validation.payment.paymentType.required")
        Integer paymentTypeId,

        @DecimalMin(value = "0.00", message = "validation.payment.paymentProcessingFees.min")
        @Digits(integer=10, fraction=2, message = "validation.payment.paymentProcessingFees.format")
        BigDecimal paymentProcessingFees,

        BigDecimal totalAmount,

        @Size(max = 255, message = "validation.payment.commentary.size")
        String commentary
) {
}
