package ru.utlc.financialmanagementservice.dto.servicetype;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.utlc.financialmanagementservice.localization.ServiceTypeLocalization;

import java.util.HashMap;
import java.util.Map;

public record ServiceTypeCreateUpdateDto(
        @NotNull(message = "validation.serviceType.name.required")
        @Pattern(regexp = ".*\\S.*", message = "validation.serviceType.name.pattern")
        @Size(min = 2, max = 100, message = "validation.serviceType.name.size")
        String name,
        String description,
        Map<String, ServiceTypeLocalization> localizations
) {
    public ServiceTypeCreateUpdateDto {
        if (localizations == null) {
            localizations = new HashMap<>();
        }
    }
}