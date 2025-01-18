package ru.utlc.financialmanagementservice.dto.invoicestatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record InvoiceStatusCreateUpdateDto(
        @NotNull(message = "{validation.invoiceStatus.name.required}")
        @Pattern(regexp = ".*\\S.*", message = "{validation.invoiceStatus.name.pattern}")
        @Size(min = 2, max = 100, message = "{validation.invoiceStatus.name.size}")
        String name
) {}