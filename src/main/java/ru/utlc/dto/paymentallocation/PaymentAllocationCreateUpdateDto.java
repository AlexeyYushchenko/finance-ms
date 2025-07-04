package ru.utlc.dto.paymentallocation;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PaymentAllocationCreateUpdateDto(
        @NotNull(message = "{validation.paymentAllocation.paymentId.required}")
        Long paymentId,

        @NotNull(message = "{validation.paymentAllocation.invoiceId.required}")
        Long invoiceId,

        @NotNull(message = "{validation.paymentAllocation.allocatedAmount.required}")
        @DecimalMin(value = "0.01", message = "{validation.paymentAllocation.allocatedAmount.min}")
        @Digits(integer = 10, fraction = 2, message = "{validation.paymentAllocation.allocatedAmount.format}")
        BigDecimal allocatedAmount
) {
}