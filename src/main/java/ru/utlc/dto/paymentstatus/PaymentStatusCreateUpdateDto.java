package ru.utlc.dto.paymentstatus;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PaymentStatusCreateUpdateDto(
        @NotNull(message = "{validation.paymentStatus.name.required}")
        @Pattern(regexp = ".*\\S.*", message = "{validation.paymentStatus.name.pattern}")
        @Size(min = 2, max = 100, message = "{validation.paymentStatus.name.size}")
        String name
) {
}
