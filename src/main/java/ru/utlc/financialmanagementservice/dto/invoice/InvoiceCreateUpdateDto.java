package ru.utlc.financialmanagementservice.dto.invoice;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoiceCreateUpdateDto(
        @NotNull(message = "validation.invoice.clientId.required")
        Integer clientId,

        @NotNull(message = "validation.invoice.serviceTypeId.required")
        Integer serviceTypeId,

        @NotNull(message = "validation.invoice.totalAmount.required")
        @DecimalMin(value = "0.01", message = "validation.invoice.totalAmount.min")
        @Digits(integer = 10, fraction = 2, message = "validation.invoice.format")
        BigDecimal totalAmount,

        @PastOrPresent(message = "validation.invoice.issueDate.pastOrPresent")
        @NotNull(message = "validation.invoice.issueDate.required")
        LocalDate issueDate,

        LocalDate dueDate,

        @Size(max = 255, message = "validation.invoice.commentary.size")
        String commentary,

        @NotNull(message = "validation.invoice.currency.required")
        Integer currencyId,

        Long shipmentId,

        @NotNull(message = "validation.invoice.invoiceStatus.required")
        Integer statusId
) {
}