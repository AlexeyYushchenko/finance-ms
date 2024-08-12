package ru.utlc.financialmanagementservice.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import ru.utlc.financialmanagementservice.localization.PaymentTypeLocalization;

public interface PaymentTypeLocalizationRepository extends R2dbcRepository<PaymentTypeLocalization, Integer> {
}
