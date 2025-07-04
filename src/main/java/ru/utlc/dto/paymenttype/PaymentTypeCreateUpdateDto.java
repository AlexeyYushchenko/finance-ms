package ru.utlc.dto.paymenttype;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PaymentTypeCreateUpdateDto(
        @NotNull(message = "{validation.paymentType.name.required}")
        @Pattern(regexp = ".*\\S.*", message = "{validation.paymentType.name.pattern}")
        @Size(min = 2, max = 100, message = "{validation.paymentType.name.size}")
        String name,
        String description
) {}