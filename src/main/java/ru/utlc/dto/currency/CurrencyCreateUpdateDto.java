package ru.utlc.dto.currency;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CurrencyCreateUpdateDto(
        @NotNull(message = "{validation.currency.code.required}")
        @Pattern(regexp = "^[A-Z]{3,4}$", message = "{validation.currency.code.pattern}")
        String code,

        @NotNull(message = "{validation.currency.okvCode.required}")
        @Pattern(regexp = "^[0-9]{3}$", message = "{validation.currency.okvCode.pattern}")
        String okvCode,

        @Size(min = 2, max = 50, message = "{validation.currency.name.size}")
        String name,

        Boolean enabled
) {
}
