package ru.utlc.financialmanagementservice.dto.paymenttype;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.utlc.financialmanagementservice.localization.InvoiceStatusLocalization;
import ru.utlc.financialmanagementservice.localization.PaymentTypeLocalization;

import java.util.HashMap;
import java.util.Map;

public record PaymentTypeCreateUpdateDto(
        @NotNull(message = "{validation.paymentType.name.required}")
        @Pattern(regexp = ".*\\S.*", message = "{validation.paymentType.name.pattern}")
        @Size(min = 2, max = 100, message = "{validation.paymentType.name.size}")
        String name,
        String description
) {}