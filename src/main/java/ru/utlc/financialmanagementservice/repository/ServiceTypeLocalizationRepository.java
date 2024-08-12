package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.utlc.financialmanagementservice.localization.ServiceTypeLocalization;

public interface ServiceTypeLocalizationRepository extends R2dbcRepository<ServiceTypeLocalization, Integer> {
}
