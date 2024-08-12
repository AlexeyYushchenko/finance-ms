package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.utlc.financialmanagementservice.localization.InvoiceStatusLocalization;

public interface InvoiceStatusLocalizationRepository extends R2dbcRepository<InvoiceStatusLocalization, Integer> {
}
